function parse_float(x)
{
	if (isNaN(parseFloat(x)) || !isFinite(x))
		throw "Invalid number: " + x;
	return +x;
}

function parse_integer(x)
{
	x = parse_float(x);
	if (Math.round(x) != x)
		throw "Invalid integer: " + x;
	return Math.round(x);
}

function process(data)
{
	// check the canvas size
	var canvas = document.getElementById("canvas");
	var y_base = 30;
	var size = canvas.height - y_base;
	var x_base = (canvas.width - size) * 0.5;
	// parse the data
	data = data.split(",");
	// parse the header
	var N = parse_integer(data[0]);
	if (data.length != 4 + N * 10)
		throw "Invalid data format: " + data.length + " != " + a;
	var side = parse_integer(data[1]);
	var clock = data[2].trim();
	var refresh = parse_integer(data[3]);
	// parse the players and their scores
	var group = new Array(N);
	var x_curr = new Array(N);
	var y_curr = new Array(N);
	var x_prev = new Array(N);
	var y_prev = new Array(N);
	var chat = new Array(N);
	var wiser = new Array(N);
	var wisdom = new Array(N);
	var rel = new Array(N);
	var score = new Array(N);
	var a = 4;
	for (var i = 0 ; i != N ; ++i) {
		group[i] = data[a++].trim();
		x_curr[i] = (parse_float(data[a++]) / side) * size + x_base;
		y_curr[i] = (parse_float(data[a++]) / side) * size + y_base;
		x_prev[i] = (parse_float(data[a++]) / side) * size + x_base;
		y_prev[i] = (parse_float(data[a++]) / side) * size + y_base;
		chat[i]   = parse_integer(data[a++])
		wiser[i]  = parse_integer(data[a++]) == 1;
		wisdom[i] = parse_integer(data[a++]);
		rel[i] = parse_integer(data[a++]);
		score[i]  = parse_integer(data[a++]);
	}
	// clear the canvas
	var ctx = canvas.getContext("2d");
	ctx.clearRect(0, 0, canvas.width, canvas.height);
	// draw the room
	ctx.beginPath();
	ctx.moveTo(x_base + 1.5,        y_base + 1.5);
	ctx.lineTo(x_base + size - 1.5, y_base + 1.5);
	ctx.lineTo(x_base + size - 1.5, y_base + size - 1.5);
	ctx.lineTo(x_base + 1.5,        y_base + size - 1.5);
	ctx.lineTo(x_base + 1.5,        y_base + 1.5);
	ctx.lineWidth = 3;
	ctx.strokeStyle = "black";
	ctx.stroke();
	// draw the clock
	ctx.font = "22px Arial";
	ctx.textAlign = "center";
	ctx.fillStyle = "black";
	ctx.fillText(clock, x_base + size * 0.5, y_base * 0.7);
	// draw the players in the room
	ctx.font = "9px Arial";
	ctx.lineWidth = 0.5;
	var radius = []; // [0.025, 0.1, 0.3];
	for (var i = 0 ; i != N ; ++i) {
		// previous position
		ctx.beginPath();
		ctx.arc(x_prev[i], y_prev[i], 3, 0, 2 * Math.PI);
		ctx.fillStyle = "red";
		ctx.fill();
		// draw line between previous and current position
		ctx.beginPath();
		ctx.moveTo(x_prev[i], y_prev[i]);
		ctx.lineTo(x_curr[i], y_curr[i]);
		ctx.strokeStyle = "red";
		ctx.stroke();
		// current position
		ctx.beginPath();
		ctx.arc(x_curr[i], y_curr[i], 3, 0, 2 * Math.PI);
		ctx.fillStyle = wiser[i] ? "blue" : "black";
		ctx.fill();
		// information text
		var text = i + ":" + group[i];
		if (wiser[i] || refresh < 0)
			text = text + ":" + wisdom[i] + ":" + score[i];
		ctx.fillText(text, x_curr[i], y_curr[i] + 10);
		// radius of influence
		ctx.strokeStyle = ctx.fillStyle;
		for (var r = 0 ; r != radius.length ; ++r) {
			ctx.beginPath();
			ctx.arc(x_curr[i], y_curr[i], radius[r] * canvas_size, 0, 2 * Math.PI);
			ctx.stroke();
		}
	}
	// draw the chat connections
	for (var i = 0 ; i != N ; ++i) {
		var j = chat[i];
		if (i != j) {
			if (rel[i] == 0)
				ctx.strokeStyle = "blue";
			else if (rel[i] == 1)
				ctx.strokeStyle = "green";
			else if (rel[i] == 2)
				ctx.strokeStyle = "magenta";
			ctx.beginPath();
			ctx.moveTo(x_curr[i], y_curr[i]);
			ctx.lineTo(x_curr[j], y_curr[j]);
			ctx.stroke();
		}
	}
	return refresh;
}

var latest_version = -1;

function ajax(version, retries, timeout)
{
	var xhr = new XMLHttpRequest();
	xhr.onload = (function() {
		var refresh = -1;
		try {
			if (xhr.readyState != 4)
				throw "Incomplete HTTP request: " + xhr.readyState;
			if (xhr.status != 200)
				throw "Invalid HTTP status: " + xhr.status;
			refresh = process(xhr.responseText);
			if (latest_version < version)
				latest_version = version;
			else
				refresh = -1;
		} catch (message) { alert(message); }
		if (refresh >= 0)
			setTimeout(function() { ajax(version + 1, 10, 100); }, refresh);
	});
	xhr.onabort   = (function() { location.reload(true); });
	xhr.onerror   = (function() { location.reload(true); });
	xhr.ontimeout = (function() {
		if (version <= latest_version)
			console.log("AJAX timeout (version " + version + " <= " + latest_version + ")");
		else if (retries == 0)
			location.reload(true);
		else {
			console.log("AJAX timeout (version " + version + ", retries: " + retries + ")");
			ajax(version, retries - 1, timeout * 2);
		}
	});
	xhr.open("GET", "data.txt", true);
	xhr.responseType = "text";
	xhr.timeout = timeout;
	xhr.send();
}

ajax(0, 10, 100);
