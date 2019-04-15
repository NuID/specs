# nuid.specs

Cross-platform public [specs](https://clojure.org/about/spec) common across NuID APIs.

## Requirements

[`jvm`](https://www.java.com/en/download/), [`clj`](https://clojure.org/guides/getting_started)

## Notes:

By isolating `nuid.specs`, we trade-off localization for generality: each spec in `nuid.specs` exists independently of (and removed from) context so that it may be `required` and applied anywhere without risk of circular dependency.

These specs will update as needed to reflect API changes. Many of the specs have been designed to benefit from the [changes](https://github.com/clojure/spec-alpha2) coming to spec to disambiguate schema and selection. This library exists as early groundwork in adding arbitrarily powerful, programmatically actionable rigor to public NuID APIs.

## From Clojure and ClojureScript

`clojure.spec` is available to both Clojure and ClojureScript, so this library can be used from either.

### tools.deps:

`{nuid/specs {:git/url "https://github.com/nuid/specs" :sha "..."}}`

### usage:

```
$ clj
=> (require '[clojure.spec.alpha :as s])
=> (require '[nuid.specs :as specs])

=> (s/valid? ::specs/protocol {:id :knizk}) ;; => true
=> (s/valid? ::specs/protocol {:id :bad})   ;; => false

=> (s/valid? ::specs/credential
     {:protocol {:id :knizk}
      :curve {:id :secp256k1}
      :keyfn {...} ;; e.g. nuid.cryptography/generate-scrypt-parameters
      :hashfn {:id :sha256 :normalization-form "NFKC"}}) ;; => true
```

### notes:

This library is still in the alpha phase of describing the shape of the data that flows between prover and verifier (as well as between more specific components in the system). More use and additional consumers will help improve and anneal these specifications.

It is important to remember that this library only ever describes ("specifies") pure data. E.g. `nuid.specs/hashfn` describes a data format that can be used by `nuid.cryptography/generate-hashfn` to produce an actual function for hashing values. It is helpful to mentally append `-spec` to the `s/def`'s described in `nuid.specs`, which in practice would be annoyingly redundant.

## Contributing

Install [`git-hooks`](https://github.com/icefox/git-hooks) and fire away.

### formatting:

```
$ clojure -A:cljfmt            # check
$ clojure -A:cljfmt:cljfmt/fix # fix
```

### dependencies:

```
## check
$ clojure -A:depot

## update
$ clojure -A:depot:depot/update
```
