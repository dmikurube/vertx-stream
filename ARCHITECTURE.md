References
-----------

### Vert.x

#### Streaming the response

* [Adapter from java.util.Stream io.vertx.core.streams.ReadStream](https://groups.google.com/g/vertx/c/lEJ2nScWSa8) (Vert.x Google Group, 2021-12-14)
    * ↑ May not be working fine...?
* [How to create a Vert.x ReadStream from an InputStream](https://stackoverflow.com/questions/66158455/how-to-create-a-vert-x-readstream-from-an-inputstream)  (Stack Overflow, 2021-02-11)
* [Streaming S3 object to VertX Http Server Response](https://stackoverflow.com/questions/51664126/streaming-s3-object-to-vertx-http-server-response) (Stack Overflow, 2018-08-03)
* [Wrapping an InputStream into a ReadStream<Buffer> for vert.x](https://gist.github.com/Stwissel/a7f8ce79785afd49eb2ced69b56335de) (GitHub Gist, 2016-04-24)
    * ↑ Good reference!

#### Streaming in EventBus

Streaming through EventBus is still impossible with Vert.x 4.

* [Streaming a reply through the eventbus?](https://groups.google.com/g/vertx/c/KX0qopBJoTo) (Vert.x Google Group, 2014-05-22)

The plan for Vert.x 5 includes "event streaming support on Vert.x event-bus to support the streaming use case".

* [Vert.x 5](https://github.com/eclipse-vertx/vert.x/wiki/Vert.x-5)

See other references below.

* [Vertx: request/response with response stream](https://stackoverflow.com/questions/76279459/vertx-request-response-with-response-stream) (Stack Overflow, 2023-05-18)
* [Is the performance of Vertx event bus as good or better than ConcurrentQueues in Java?](https://stackoverflow.com/questions/63014319/is-the-performance-of-vertx-event-bus-as-good-or-better-than-concurrentqueues-in) (Stack Overflow, 2020-07-21)
* [A Streaming Pattern for the vert.x EventBus (Part 1)](https://www.wissel.net/blog/2019/12/a-streaming-pattern-for-the-vert.x-eventbus.html) (2019-12-04)
* [What is the best way to send a collection of objects over the Event Bus in Vertx?](https://stackoverflow.com/questions/58941036/what-is-the-best-way-to-send-a-collection-of-objects-over-the-event-bus-in-vertx) (Stack Overflow, 2019-11-19)
* [Is it wise to stream file over vertx event bus](https://stackoverflow.com/questions/51665264/is-it-wise-to-stream-file-over-vertx-event-bus) (Stack Overflow, 2018-08-03)
* [Sending a file to vertx eventbus](https://stackoverflow.com/questions/44033272/sending-a-file-to-vertx-eventbus) (Stack Overflow, 2017-05-17)
