package wtr.sim;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.tools.*;
import java.awt.Desktop;
import java.util.concurrent.*;

class Simulator {

	private static final String root = "wtr";

	public static void main(String[] args)
	{
		int friends = 10;
		int strangers = 88;
		boolean verbose = false;
		boolean recompile = false;
		long init_timeout = 1000;
		long play_timeout = 1000;
		int room_side = 20;
		int turns = 1800;
		boolean gui = false;
		long gui_refresh = 1000;
		Class[] classes = null;
		String[] groups = null;
		HashSet <String> group_set = new HashSet <String> ();
		group_set.add("g0");
		try {
			for (int a = 0 ; a != args.length ; ++a)
				if (args[a].equals("-f") || args[a].equals("--friends")) {
					if (++a == args.length)
						throw new IllegalArgumentException("Missing number of friends");
					friends = Integer.parseInt(args[a]);
					if (friends < 0)
						throw new IllegalArgumentException("Invalid number of friends");
				} else if (args[a].equals("-s") || args[a].equals("--strangers")) {
					if (++a == args.length)
						throw new IllegalArgumentException("Missing number of strangers");
					strangers = Integer.parseInt(args[a]);
					if (strangers < 0)
						throw new IllegalArgumentException("Invalid number of strangers");
				} else if (args[a].equals("-g") || args[a].equals("--groups")) {
					group_set.clear();
					while (a + 1 != args.length && !args[a + 1].startsWith("-"))
						if (group_set.add(args[++a]) == false)
							throw new IllegalArgumentException("Repeated group (player)");
					if (group_set.size() == 0)
						throw new IllegalArgumentException("Missing groups (players)");
				} else if (args[a].equals("--fps")) {
					if (++a == args.length)
						throw new IllegalArgumentException("Missing FPS");
					double gui_fps = Double.parseDouble(args[a]);
					gui_refresh = gui_fps > 0.0 ? (long) Math.round(1000.0 / gui_fps) : -1;
					gui = true;
				} else if (args[a].equals("--gui")) gui = true;
				else if (args[a].equals("--recompile")) recompile = true;
				else if (args[a].equals("--verbose")) verbose = true;
				else throw new IllegalArgumentException("Unknown argument: " + args[a]);
			int N = friends + strangers + 2;
			if (group_set.size() > N)
				throw new IllegalArgumentException("Too many distinct groups");
			if (N % group_set.size() != 0)
				throw new IllegalArgumentException("Participants not divisible by players");
			int instances = N / group_set.size();
			List <String> group_list = new ArrayList <String> ();
			for (String group : group_set)
				for (int i = 0 ; i != instances ; ++i)
					group_list.add(group);
			groups = group_list.toArray(new String [0]);
			classes = load(groups, recompile);
		} catch (Exception e) {
			System.err.println("Error during setup: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
		int N = friends + strangers + 2;
		// print info
		System.err.println("Friends: " + friends);
		System.err.println("Strangers: " + strangers);
		System.err.print("Players (distinct): ");
		for (String group : group_set)
			System.err.print(" " + group);
		System.err.println("");
		System.err.println("Instances (per player): " + N / group_set.size());
		System.err.println("Verbose: " + (verbose ? "yes" : "no"));
		System.err.println("Recompile: " + (recompile ? "yes" : "no"));
		if (!gui)
			System.err.println("GUI: disabled");
		else if (gui_refresh < 0)
			System.err.println("GUI: enabled  (0 FPS)  [reload manually]");
		else if (gui_refresh == 0)
			System.err.println("GUI: enabled  (max FPS)");
		else {
			double fps = 1000.0 / gui_refresh;
			System.err.println("GUI: enabled  (up to " + fps + " FPS)");
		}
		// start game
		int[] score = new int [N];
		boolean[] timeout = new boolean [N];
		try {
			game(groups, classes, friends, strangers,
			     room_side, turns, score, timeout,
			     init_timeout, play_timeout,
			     gui, gui_refresh, verbose);
		} catch (Exception e) {
			System.err.println("Error during the game: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
		for (int i = 0 ; i != score.length ; ++i)
			System.err.println("Player " + (i + 1) + " scored: " + score[i]);
		System.exit(0);
	}

	private static final Random random = new Random();

	private static void game(String[] groups, Class[] classes,
	                         int friends, int strangers,
	                         int room_side, int turns,
	                         int[] score, boolean[] timeout,
	                         long init_timeout, long play_timeout,
	                         boolean gui, long gui_refresh,
	                         boolean verbose) throws Exception
	{
		int N = friends + strangers + 2;
		if (classes.length != N || groups.length != N ||
		    score.length != N || timeout.length != N)
			throw new IllegalArgumentException();
		PrintStream out = verbose ? System.out : null;
		// initial random locations
		Point[] Lp = new Point [N];
		Point[] L  = new Point [N];
		for (int i = 0 ; i != N ; ++i) {
			int b = 1000 * 1000 * 1000;
			double x = random.nextInt(b + 1) * 20.0 / b;
			double y = random.nextInt(b + 1) * 20.0 / b;
			L[i] = Lp[i] = new Point(x, y, i);
		}
		// initialize wisdom and friends array
		int[][] W = wisdom(friends, strangers);
		System.err.println("Wisdom array generated");
		boolean[][] F = new boolean [N][N];
		int[] Sm = new int [N];
		for (int i = 0 ; i != N ; ++i)
			for (int j = 0 ; j != i ; ++j)
				if (W[i][j] == 50) {
					verify(W[j][i] == 50);
					F[i][j] = F[j][i] = true;
				} else if (W[i][j] == 200) {
					verify(W[j][i] == 200);
					Sm[i] = j;
					Sm[j] = i;
				}
		// initialize players
		Timer[] threads = new Timer [N];
		Player[] players = new Player [N];
		for (int i = 0 ; i != N ; ++i) {
			int[] friend_ids = new int [friends];
			for (int j = 0, k = 0 ; j != N ; ++j)
				if (F[i][j])
					friend_ids[k++] = j;
			Class player_class = classes[i];
			int id = i;
			threads[i] = new Timer();
			threads[i].start();
			players[i] = threads[i].call(new Callable <Player> () {

				public Player call() throws Exception
				{
					Player p = (Player) player_class.newInstance();
					p.init(id, friend_ids, strangers);
					return p;
				}
			}, init_timeout);
		}
		// play the game
		Point[] M = new Point [N];
		boolean[] C = new boolean [N];
		for (int i = 0 ; i != N ; ++i) {
			score[i] = 0;
			timeout[i] = false;
			M[i] = new Point(0.0, 0.0, i);
		}
		// initialize gui
		HTTPServer server = null;
		if (gui) {
			server = new HTTPServer();
			System.err.println("HTTP port: " + server.port());
			// try to open web browser automatically
			if (!Desktop.isDesktopSupported())
				System.err.println("Desktop operations not supported");
			else if (!Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
				System.err.println("Desktop browsing not supported");
			else {
				URI uri = new URI("http://localhost:" + server.port());
				Desktop.getDesktop().browse(uri);
			}
		}
		for (int turn = 0 ; turn != turns ; ++turn) {
			String clock = clock(turn * 6);
			// GUI state
			if (gui) gui(server, state(groups, L, Lp, score, W, C, F, Sm,
			                           room_side, clock, gui_refresh));
			if (out != null) println(out, clock);
			else System.err.println(clock);
			// for each player
			for (int i = 0 ; i != N ; ++i) {
				// find which players are in view
				int k = 0;
				for (int j = 0 ; j != N ; ++j)
					if (L[i].distance(L[j]) <= 6.0) k++;
				Point[] V = new Point [k];
				int[] Vc = new int [k];
				for (int j = k = 0 ; j != N ; ++j)
					if (L[i].distance(L[j]) <= 6) {
						Vc[k] = L[i].distance(L[M[j].id]) > 6 ? -1 : M[j].id;
						V[k++] = new Point(L[j].x, L[j].y, j);
					}
				println(out, i + " views " + (k - 1) + " people from ("
				                         + L[i].x + ", " + L[i].y + ")");
				// get next move from player
				int j = M[i].id;
				boolean wiser = C[i];
				int more_wisdom = F[j][i] ? -1 : W[j][i];
				Player player = players[i];
				M[i] = threads[i].call(new Callable <Point> () {

					public Point call() throws Exception
					{
						return player.play(V, Vc, wiser, more_wisdom);
					}
				}, play_timeout);
				// invalid moves become stay put
				Point m = M[i];
				M[i] = null;
				C[i] = false;
				if (m == null)
					println(out, i + ": Unspecified action");
				else if (Double.isNaN(m.x) || Double.isInfinite(m.x))
					println(out, i + ": Undefined movement x");
				else if (Double.isNaN(m.y) || Double.isInfinite(m.y))
					println(out, i + ": Undefined movement y");
				else if (m.id < 0 || m.id >= N)
					println(out, i + ": Invalid chat target: " + m.id);
				else if (m.id != i && (m.x != 0 || m.y != 0))
					println(out, i + ": Cannot move and chat simultaneously");
				else if (m.id != i && L[i].id != i && L[i].id != m.id)
					println(out, i + ": Cannot initiate chat yet");
				else if (m.id != i && L[i].distance(L[m.id]) < 0.5001)
					println(out, i + ": Chat target too close to chat");
				else if (m.id != i && L[i].distance(L[m.id]) > 2.0001)
					println(out, i + ": Chat target too far to chat");
				else if (m.id == i && L[i].x + m.x < 0)
					println(out, i + ": Invalid movement: dx < 0");
				else if (m.id == i && L[i].y + m.y < 0)
					println(out, i + ": Invalid movement: dy < 0");
				else if (m.id == i && L[i].x + m.x > room_side)
					println(out, i + ": Invalid movement: dx > " + room_side);
				else if (m.id == i && L[i].y + m.y > room_side)
					println(out, i + ": Invalid movement: dy > " + room_side);
				else if (m.id == i && Math.hypot(m.x, m.y) > 6.0001)
					println(out, i + ": Movement too fast: " +
					                  Math.hypot(m.x, m.y));
				else M[i] = m;
				// player has to stay put
				if (M[i] == null)
					M[i] = new Point(0.0, 0.0, i);
			}
			// both players want to continue chatting (rule 1)
			for (int i = 0 ; i != N ; ++i) {
				int j = M[i].id;
				if (i < j && M[j].id == i && L[i].id == j) {
					verify(!C[i] && !C[j] && L[j].id == i);
					C[i] = C[j] = true;
					println(out, i + " and " + j + " want to continue chatting");
				}
			}
			// both players want to start chatting (rule 2)
			for (int i = 0 ; i != N ; ++i) {
				int j = M[i].id;
				if (i < j && M[j].id == i && L[i].id == i) {
					verify(!C[i] && !C[j] && L[j].id == j);
					C[i] = C[j] = true;
					println(out, i + " and " + j + " want to start chatting");
				}
			}
			// gather players that try to initiate chat (rule 3)
			List <Pair> Sl = new ArrayList <Pair> ();
			for (int i = 0 ; i != N ; ++i) {
				int j = M[i].id;
				if (i != j && M[j].id != i && L[i].id == i) {
					verify(!C[i]);
					Sl.add(new Pair(i, L[i].distance(L[j])));
				}
			}
			// shuffle before sorting for random handling of equals
			Pair[] S = Sl.toArray(new Pair [0]);
			for (int i = 0 ; i != S.length ; ++i) {
				int j = random.nextInt(S.length - i) + i;
				Pair p = S[i];  S[i] = S[j];  S[j] = p;
			}
			Arrays.sort(S);
			// player tries to initiate conversation (rule 3)
			for (Pair s : S) {
				int i = s.i;
				int j = M[i].id;
				verify(i != j && L[i].id == i);
				// your target is idle and he has to be polite
				if (!C[i] && !C[j] && L[j].id == j) {
					C[i] = C[j] = true;
					M[j] = new Point(0.0, 0.0, i);
					println(out, j + " was forced to start chatting with " + i);
				}
			}
			// player failed to initiate conversation (rule 3)
			for (Pair s : S) {
				int i = s.i;
				int j = M[i].id;
				// player has to stay put
				if (!C[i]) M[i] = new Point(0.0, 0.0, i);
				println(out, i + " failed to start chat with " + j);
			}
			// target doesn't want to continue conversation (rule 4)
			for (int i = 0 ; i != N ; ++i) {
				int j = M[i].id;
				if (i != j && L[i].id == j && M[j].id != i) {
					verify(!C[i] && L[j].id == i && M[j].id == j);
					M[i] = new Point(0.0, 0.0, i);
					println(out, j + " wants to stop chatting with " + i);
				}
			}
			// player wants to move or has to stay put (rule 5)
			for (int i = 0 ; i != N ; ++i)
				if (M[i].id == i) {
					verify(!C[i]);
					C[i] = true;
					println(out, i + " moved from (" + L[i].x + ", " + L[i].y + ")"
					 + " to (" + (L[i].x + M[i].x) + ", " + (L[i].y + M[i].y) + ")");
				}
			// move all players that must now be processed
			for (int i = 0 ; i != N ; ++i) {
				verify(C[i]);
				C[i] = false;
				Lp[i] = L[i];
				L[i] = new Point(L[i].x + M[i].x, L[i].y + M[i].y, M[i].id);
			}
			// validate pair-wise conversations
			for (int i = 0 ; i != N ; ++i) {
				int j = L[i].id;
				if (i == j) continue;
				verify(L[j].id == i);
			}
			// update wisdom points
			for (int i = 0 ; i != N ; ++i) {
				// not conversing or no wisdom left
				int j = L[i].id;
				if (i == j) continue;
				if (W[j][i] == 0) {
					println(out, i + " has no more wisdom to gain from " + j);
					continue;
				}
				// search for closest player
				double d = L[i].distance(L[j]);
				boolean c = true;
				for (int k = 0 ; k != N && c ; ++k)
					if (i != k && j != k && L[i].distance(L[k]) < d)
						c = false;
				// gain wisdom if closest
				if (!c) println(out, i + " cannot gain wisdom from " + j);
				else {
					W[j][i]--;
					C[i] = true;
					score[i]++;
					println(out, i + " gained wisdom from " + j);
				}
			}
		}
		if (gui) {
			gui(server, state(groups, L, Lp, score, W, C, F, Sm,
			                  room_side, clock(turns * 6), -1));
			server.close();
		}
	}

	private static String clock(int seconds)
	{
		int hour =     seconds / 3600;
		int minutes_hi =  (seconds % 3600) / 600;
		int minutes_lo = ((seconds % 3600) / 60) % 10;
		int seconds_hi =  (seconds         % 60) / 10;
		int seconds_lo =   seconds               % 10;
		return hour + ":" + minutes_hi + "" + minutes_lo
		            + ":" + seconds_hi + "" + seconds_lo;
	}

	private static void verify(boolean x)
	{
		if (x == false) throw new AssertionError();
	}

	private static class Pair implements Comparable <Pair> {

		public final int i;
		public final double d;

		public Pair(int i, double d)
		{
			this.i = i;
			this.d = d;
		}

		public int compareTo(Pair p)
		{
			return d < p.d ? -1 : (d > p.d ? 1 : 0);
		}
	}

	private static int[][] wisdom(int friends, int strangers)
	{
		if (friends < 0 || strangers < 0)
			throw new IllegalArgumentException();
		// enforce symmetries
		int N = friends + strangers + 2;
		// generate the graph of friends and soulmates
		boolean[][] F = rand_und_graph(N, friends + 1);
		// initialize wisdom array using the graph
		int[][] W = new int [N][N];
		for (int i = 0 ; i != N ; ++i)
			for (int j = 0 ; j != N ; ++j)
				W[i][j] = F[i][j] ? 50 : -1;
		// create a random 1:1 mapping [0,N) -> [0,N)
		int[] M = new int [N];
		for (int i = 0 ; i != N ; ++i)
			M[i] = i;
		for (int i = 0 ; i != N ; ++i) {
			int j = random.nextInt(N - i) + i;
			int t = M[i];
			M[i] = M[j];
			M[j] = t;
		}
		// shuffle graph nodes using the random 1:1 mapping
		boolean[][] Fm = new boolean [N][N];
		for (int i = 0 ; i != N ; ++i)
			for (int j = 0 ; j != N ; ++j)
				if (F[i][j]) {
					int mi = M[i];
					int mj = M[j];
					Fm[mi][mj] = true;
				}
		// convert graph to adjacency list
		int[][] G = new int [N][];
		for (int i = 0 ; i != N ; ++i) {
			int k = 0;
			for (int j = 0 ; j != N ; ++j)
				if (Fm[i][j]) k++;
			G[i] = new int [k];
			for (int j = k = 0 ; j != N ; ++j)
				if (Fm[i][j]) G[i][k++] = j;
		}
		// find an edge cover using Edmond's max matching algorithm
		int[] C = Edmonds.matching(G);
		// convert edge cover to original graph and update wisdom
		for (int mi = 0 ; mi != N ; ++mi) {
			int mj = C[mi];
			verify(C[mj] == mi);
			if (mi < mj) {
				int i = 0, j = 0;
				while (M[i] != mi) i++;
				while (M[j] != mj) j++;
				verify(F[i][j] && F[j][i]);
				verify(W[i][j] == 50 && W[j][i] == 50);
				F[i][j] = F[j][i] = false;
				W[i][j] = W[j][i] = 200;
			}
		}
		// generate how many strangers give 0, 10, and 20 points
		int[] S = new int [] {0, 0, 0};
		for (int s = 0 ; s != strangers ; ++s)
			S[random.nextInt(3)]++;
		// distribute the stranger classes to players
		for (int c = 0 ; c != 3 ; ++c)
			for (int s = 0 ; s != S[c] ; ++s)
				for (int i = 0 ; i != N ; ++i) {
					int k = 0;
					for (int j = 0 ; j != N ; ++j)
						if (W[j][i] < 0) k++;
					k = random.nextInt(k) + 1;
					for (int j = 0 ; j != N ; ++j)
						if (W[j][i] < 0 && --k == 0) {
							W[j][i] = c * 10;
							break;
						}
				}
		return W;
	}

	private static boolean[][] rand_und_graph(int nodes, int degrees)
	{
		if (nodes <= 0 || degrees <= 0 || degrees >= nodes)
			throw new IllegalArgumentException();
		// all node connections (symmetric)
		boolean[][] C = new boolean [nodes][nodes];
		for (int d = 0 ; d != degrees ; ++d) {
			// mark nodes you connect per turn
			boolean[] M = new boolean [nodes];
			int j, k = 0;
			for (int i = 0 ; i != nodes ; ++i) {
				// skip marked nodes
				if (M[i]) continue;
				// find all unmarked disconnected nodes
				for (j = 0 ; j != nodes ; ++j)
					if (i != j && !M[j] && !C[i][j]) k++;
				// no disconnected nodes
				if (k == 0) {
					int i2 = 0, j2 = 0, k2 = 0;
					// count all unmarked connected nodes
					for (j = 0 ; j != nodes ; ++j)
						if (i != j && !M[j] && C[i][j]) k2++;
					do {
						// pick a unmarked connected node
						k = random.nextInt(k2) + 1;
						for (j = 0 ;; ++j)
							if (!M[j] && C[i][j] && --k == 0) break;
						// find all pairs of marked connected nodes
						for (i2 = 0 ; i2 != nodes ; ++i2) {
							if (!M[i2]) continue;
							for (j2 = 0 ; j2 != nodes ; ++j2) {
								if (!M[j2] || !C[i2][j2]) continue;
								if (!C[i][i2] && !C[j][j2]) k++;
							}
						}
					// if no marked pair retry with another unmarked pair
					} while (k == 0);
					// pick a pair of marked connected nodes
					k = random.nextInt(k) + 1;
					nested_loop:
					for (i2 = 0 ; i2 != nodes ; ++i2) {
						if (!M[i2]) continue;
						for (j2 = 0 ; j2 != nodes ; ++j2) {
							if (!M[j2] || !C[i2][j2]) continue;
							if (!C[i][i2] && !C[j][j2] && --k == 0)
								break nested_loop;
						}
					}
					// disconnect one pair of nodes
					C[i2][j2] = C[j2][i2] = false;
					// connect two pairs of nodes
					C[i][i2] = C[i2][i] = true;
					C[j][j2] = C[j2][j] = true;
					// 
				} else {
					// pick a disconnected node
					k = random.nextInt(k) + 1;
					for (j = 0 ;; ++j)
						if (i != j && !M[j] && !C[i][j] && --k == 0) break;
					// connect the disconnected nodes
					C[i][j] = C[j][i] = true;
				}
				// mark connected nodes
				M[i] = M[j] = true;
			}
		}
		return C;
	}

	private static void print(PrintStream out, String message)
	{
		if (out != null) out.print(message);
	}

	private static void println(PrintStream out, String message)
	{
		if (out != null) out.println(message);
	}

	private static String toString(int number, int digits, boolean zero)
	{
		byte[] bytes = new byte [digits];
		do {
			bytes[--digits] = (byte) (number % 10 + '0');
			number /= 10;
		} while (number != 0);
		while (digits != 0)
			bytes[--digits] = (byte) (zero ? '0' : ' ');
		return new String(bytes);
	}

	private static String state(String[] groups,
	                            Point[] locations,
	                            Point[] previous_locations,
	                            int[] score,
	                            int[][] wisdom,
	                            boolean[] wiser,
	                            boolean[][] friends,
	                            int[] soulmates,
	                            int side,
	                            String clock,
	                            long gui_refresh)
	{
		int N = groups.length;
		StringBuffer buf = new StringBuffer();
		buf.append(N + ", " + side + ", " + clock + ", " + gui_refresh);
		for (int i = 0 ; i != N ; ++i) {
			int j = locations[i].id;
			buf.append(", " + groups[i] +
			           ", " + locations[i].x +
			           ", " + locations[i].y +
			           ", " + previous_locations[i].x +
			           ", " + previous_locations[i].y +
			           ", " + j +
			           ", " + (wiser[i] ? 1 : 0) +
			           ", " + wisdom[j][i] +
			           ", " + (friends[i][j] ? 1 : (j == soulmates[i] ? 2 : 0)) +
			           ", " + score[i]);
		}
		return buf.toString();
	}

	private static void gui(HTTPServer server, String content)
	                        throws UnknownServiceException
	{
		String path = null;
		for (;;) {
			// get request
			for (;;)
				try {
					path = server.request();
					break;
				} catch (IOException e) {
					System.err.println("HTTP request error: " + e.getMessage());
				}
			// dynamic content
			if (path.equals("data.txt"))
				// send dynamic content
				try {
					server.reply(content);
					return;
				} catch (IOException e) {
					System.err.println("HTTP dynamic reply error: " + e.getMessage());
					continue;
				}
			// static content
			if (path.equals("")) path = "webpage.html";
			else if (!path.equals("favicon.ico") &&
			         !path.equals("apple-touch-icon.png") &&
			         !path.equals("script.js")) break;
			// send file
			File file = new File(root + File.separator + "sim"
			                          + File.separator + path);
			try {
				server.reply(file);
			} catch (IOException e) {
				System.err.println("HTTP static reply error: " + e.getMessage());
			}
		}
		if (path == null)
			throw new UnknownServiceException("Unknown HTTP request (null path)");
		else
			throw new UnknownServiceException("Unknown HTTP request: \"" + path + "\"");
	}

	private static Set <File> directory(String path, String extension)
	{
		Set <File> files = new HashSet <File> ();
		Set <File> prev_dirs = new HashSet <File> ();
		prev_dirs.add(new File(path));
		do {
			Set <File> next_dirs = new HashSet <File> ();
			for (File dir : prev_dirs)
				for (File file : dir.listFiles())
					if (!file.canRead()) ;
					else if (file.isDirectory())
						next_dirs.add(file);
					else if (file.getPath().endsWith(extension))
						files.add(file);
			prev_dirs = next_dirs;
		} while (!prev_dirs.isEmpty());
		return files;
	}

	private static Class[] load(String[] groups, boolean compile)
	        throws IOException, ReflectiveOperationException
	{
		// get unique player sources
		Map <String, Class> group_map = new HashMap <String, Class> ();
		for (int g = 0 ; g != groups.length ; ++g)
			group_map.put(groups[g], null);
		// compile and load classes
		ClassLoader loader = Simulator.class.getClassLoader();
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		StandardJavaFileManager manager = compiler.
		                        getStandardFileManager(null, null, null);
		String sep = File.separator;
		for (String group : group_map.keySet()) {
			String dir = root + sep + group;
			File class_file = new File(dir + sep + "Player.class");
			if (compile || !class_file.exists()) {
				File source_file = new File(dir + sep + "Player.java");
				if (!source_file.exists())
					throw new FileNotFoundException(
					          "Missing source of group " + group);
				Set <File> files = directory(dir, ".java");
				System.err.print("Compiling " + group +
				                 " (" + files.size() + " files) ... ");
				if (!compiler.getTask(null, manager, null, null, null,
				     manager.getJavaFileObjectsFromFiles(files)).call())
					throw new IOException(
					          "Cannot compile source of " + group);
				System.err.println("done!");
				class_file = new File(dir + sep + "Player.class");
				if (!class_file.exists())
					throw new FileNotFoundException(
					          "Missing class of group " + group);
			}
			Class player = loader.loadClass(root + "." + group + ".Player");
			group_map.replace(group, player);
		}
		// map to players
		Class[] classes = new Class [groups.length];
		for (int g = 0 ; g != groups.length ; ++g)
			classes[g] = group_map.get(groups[g]);
		return classes;
	}
}
