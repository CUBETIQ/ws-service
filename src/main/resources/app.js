let stompClient = null;

function connect() {
    const socket = new SockJS('/wired');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        stompClient.subscribe('/topic/welcome', function (greeting) {
            console.log("Received", JSON.parse(greeting.body).content)
        });
    });
}

function disconnect() {
    if (stompClient !== null) {
        stompClient.disconnect();
    }
}

function sendName() {
    stompClient.send("/send/messaging/hi", {}, JSON.stringify({'name': $("#name").val()}));
}