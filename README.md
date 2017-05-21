# self-host-etl-pipeline

A translation of [Building ETL pipelines with Clojure and transducers](https://tech.grammarly.com/blog/building-etl-pipelines-with-clojure) to self-hosted ClojureScript.

Motivation: I was curious if this code could be cleanly translated to self-host, using Planck's [IO facilities](http://planck-repl.org/planck-io.html) and what the performance difference might be for that environment when using transducers.

The code is in the [etl-pipeline.core](https://github.com/mfikes/self-host-etl-pipeline/blob/master/src/etl_pipeline/core.cljs) namespace.

# Usage

Start up Planck, setting it to use `src` for code:

```
$ planck -c src
```

Load the code and change to the namespace:

```
(require 'etl-pipeline.core)
(in-ns 'etl-pipeline.core)
```

Create a dummy JSON file:

```
(create-file)
```

Time processing without transducers:

```
(time (process ["/tmp/dummy.json"]))
(time (process (repeat 8 "/tmp/dummy.json")))
```

Time processing with transducers:

```
(time (process-with-transducers ["/tmp/dummy.json"]))
(time (process-with-transducers (repeat 8 "/tmp/dummy.json")))
```

# Comparison

Processing without transducers:

1 file:<br/>
Clojure: 2857.870524 msecs<br/>
Self-host: 8620.306281 msecs<br/>

8 files:<br/>
Clojure: 29106.211138 msecs<br/>
Self-host: 72213.714800 msecs<br/>

Processing with transducers:

1 file:<br/>
Clojure: 2595.401761 msecs<br/>
Self-host: 7374.490957 msecs<br/>

8 files:<br/>
Clojure: 19478.215058 msecs<br/>
Self-host: 60890.650729 msecs<br/>

Interestingly, Planck without transducers ends up using about 1 1/2 cores, while with transducers, it uses 1 core. (Perhaps this reflects JavaScriptCore collecting garbage in the non-transducers use case.)
