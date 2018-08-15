


[![travis](https://travis-ci.org/matgabriel/chronicle-flux.svg?branch=master)](https://travis-ci.org/matgabriel/chronicle-flux/)
[![codecov](https://codecov.io/gh/matgabriel/chronicle-flux/branch/master/graph/badge.svg)](https://codecov.io/gh/matgabriel/chronicle-flux)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/7ad90e1ea60742c2bdff2d3c00c94b4c)](https://www.codacy.com/project/matgabriel/chronicle-flux/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=matgabriel/chronicle-flux&amp;utm_campaign=Badge_Grade_Dashboard)

# chronicle-flux

## A reactive driver for chronicle-queue

This project builds on [Chronicle Queue](https://github.com/OpenHFT/Chronicle-Queue) (see also their [product](https://chronicle.software/products/queue/) page) to offer a simple reactive data store.

Chronicle Queue is a low latency library that persists everything on disk with memory mapped files (off-heap).

### Functionalities
With the provided ChronicleStore, you can:
- Store a reactive stream.
- Subscribe to the history + live updates of the store with a reactive stream (the store returns a [Flux](http://projectreactor.io/docs/core/release/reference/#flux) from project [reactor-core](https://github.com/reactor/reactor-core)).
- Replay the history with the original timing, with a time acceleration (e.g. replay at twice the speed).
- Replay the history in a loop.

### Use cases
This project can be useful for different use cases:
- event sourcing, to store all events entering an application.
- you can use it as an off-heap buffer in a reactive stream if you need to handle data bursts.
- testing, to replay the history of an application.
- performance testing, to replay the events at higher speeds.
- a simple alternative to Kafka for basic uses cases: no need to install anything, it's all persisted on disk :smiley:


## Example usage


###Create a Chronicle Store:

Please note that you must provide a way to serialize the data as binary.
The easiest solution is to use a binary serialization protocol such as [Protobuf](https://developers.google.com/protocol-buffers/), [Avro](https://avro.apache.org/docs/current/), etc. In this case the binary serialization will be supported out of the box.

The first argument is the directory path used to persist the on disk.
Since Chronicle is using memory mapped file, **you should not use a path mounted on a network file system**, for more details, have a look at the [Chronicle documentation](https://github.com/OpenHFT/Chronicle-Queue#chronicle-queue)
The 2nd and 3rd arguments are the serializer and deserializer for your data.

```java
ChronicleStore<DummyObject> chronicleStore = new ChronicleStore<>(PATH, DummyObject::toBinary, DummyObject::fromBinary);
```

###Store a stream of data

The store method will return a handle that can be used to stop the storage.
Otherwise the data stream will be store until it completes or an error is received on the stream.

```java
Flux<DummyObject> source = ... 
Disposable handle = chronicleStore.store(source);
```

###Subscribe to the store

We can subscribe to the store and print old values, as well as new values being persisted in the store.

```java
Flux<DummyObject> allValues = chronicleStore.retrieveAll();
        allValues.doOnNext(System.out::println)
                .blockLast();
```

We can also replay the history with the same timing as the original stream, and in an infinite loop:

```java
chronicleStore.replayHistory(DummyObject::timestamp)
                .withOriginalTiming()
                .inLoop()
                .doOnNext(i -> System.out.println(Instant.now() + " " + i))
                .blockLast();
```

In order to replay data with the original timing, we need to provide a function to extract the epoch time (in milliseconds) from the data.
Chronicle-flux could potentially add a timestamp to the values when they are persisted, but this would be a poor design choice since the data would be timestamped at the very end of the chain, possibly after passing through several queues. It is in general a better idea to add a timestamp on the data as soon as they enter the system in order to have an accurate time.


###Runnable demo

if you want to run some code samples, have a look at the demo folder in the test directory that contains several runnable classes.



