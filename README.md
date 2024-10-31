# viesti
> 1. sanoma, kirjallinen tai suullinen asian tiedottaminen toiselle
> 2. se, mitä esim. kirjoituksella, taideteoksella tai musiikkikappaleella yritetään välittää

Data-driven Message Dispatcher for Clojure/Script.

**STATUS**: Incubating

## Motivation

We are big fans of the [CQRS](https://martinfowler.com/bliki/CQRS.html) pattern. It enables incremental evolution of Task ans Process-based Systems and fits well with [Event-driven architectures](https://martinfowler.com/eaaDev/EventNarrative.html). CQRS has gone mainstream with [GraphQL](https://graphql.org/). For systems entirely built with Clojure, we can simplify things with a simple library like `viesti`. Builts on top of [malli](https://github.com/metosin/malli) and [reitit](https://github.com/metosin/reitit) and the lessons learned from [kekkonen](https://github.com/metosin/kekkonen) and several procect spesific tools we have built over the years.

## License

Copyright © 2022-2024 Metosin Oy and contributors.

Available under the terms of the Eclipse Public License 2.0, see `LICENSE`.
