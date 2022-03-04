# kakkonen

[![Build Status](https://img.shields.io/github/workflow/status/metosin/kakkonen/Run%20tests.svg)](https://github.com/metosin/kakkonen/actions)
[![cljdoc badge](https://cljdoc.org/badge/metosin/kakkonen)](https://cljdoc.org/d/metosin/kakkonen/)
[![Clojars Project](https://img.shields.io/clojars/v/metosin/kakkonen.svg)](https://clojars.org/metosin/kakkonen)

Data-driven Data Dispatcher for Clojure/Script.

**STATUS**: fresh new [*alpha*](#alpha)

<img src="https://raw.githubusercontent.com/metosin/kakkonen/master/docs/img/kakkonen.png" width=130 align="right"/>

## Motivation

We are big fans of the [CQRS](https://martinfowler.com/bliki/CQRS.html) pattern. It enables incremental evolution of Task ans Process-based Systems and fits well with [Event-driven architectures](https://martinfowler.com/eaaDev/EventNarrative.html). CQRS has gone mainstream with [GraphQL](https://graphql.org/). For systems entirely built with Clojure, we can simplify things with a simple library like `kakkonen`. Builts on top of [malli](https://github.com/metosin/malli) and [reitit](https://github.com/metosin/reitit) and the lessons learned from [kekkonen](https://github.com/metosin/kekkonen) and several procect spesific tools we have built over the years.

| kakkonen > kekkonen

## The library

[![Clojars Project](http://clojars.org/metosin/kakkonen/latest-version.svg)](http://clojars.org/metosin/kakkonen)

Kakkonen requires Clojure 1.10+ and is tested against 1.10 and 1.11.

## Links (and thanks)

- kekkonen https://github.com/metosin/kekkonen
- malli https://github.com/metosin/malli
- re-frame https://github.com/day8/re-frame

## Alpha

All changes (breaking or not) will be documented in the [CHANGELOG](CHANGELOG.md) and there will be a migration guide and path if needed.

The API layers and stability:

* **public API**: public vars, name doesn't start with `-`, e.g. `kakkonen.core/dispatch`. The most stable part of the library, should not change (much) in alpha
* **extender API**: public vars, name starts with `-`, e.g. `kakkonen.core/-proxy`. Not needed with basic use cases, might evolve during the alpha, follow [CHANGELOG](CHANGELOG.md) for details
* **private API**: private vars and `malli.impl` namespaces, all bets are off.

## Running tests

We use [Kaocha](https://github.com/lambdaisland/kaocha) and [cljs-test-runner](https://github.com/Olical/cljs-test-runner) as a test runners. Before running the tests, you need to install NPM dependencies.

```bash
./bin/kaocha
./bin/node
```

## Installing locally

```bash
clj -Mjar
clj -Minstall
```

## Formatting the code

```bash
clojure-lsp format
clojure-lsp clean-ns
```

## License

Copyright © 2022 Metosin Oy and contributors.

Available under the terms of the Eclipse Public License 2.0, see `LICENSE`.
