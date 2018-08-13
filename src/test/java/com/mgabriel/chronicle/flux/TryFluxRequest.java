package com.mgabriel.chronicle.flux;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * @author mgabriel.
 */
public class TryFluxRequest {

    public static void main(String[] args) throws Exception {

        Flux<String> source = Flux.<String>create(sink -> {
                    int i = 0;
                    while (!sink.isCancelled()) {
                        if (sink.requestedFromDownstream() > 0) {
                            sink.next("" + i++);
                            if(i == 7){
                                sink.complete();
                            }
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                System.out.println("interrupted");
                            }
                        }
                    }
                    System.out.println("CANCELLED!!!!!!!!!!!!!");
                }
        );

        System.out.println("max val =" + Long.MAX_VALUE);
        Disposable sub = source.doOnRequest(r -> System.out.println("~~~~~ requested " + r))
                //.take(20)
                .doOnNext(i -> System.out.println(i))
                .subscribeOn(Schedulers.newSingle("lpop"))
                .subscribe();

        Thread.sleep(10_000);
        sub.dispose();
        System.out.println("disposed");
    }
}
