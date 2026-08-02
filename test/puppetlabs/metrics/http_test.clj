(ns puppetlabs.metrics.http-test
  (:import (io.dropwizard.metrics5 MetricName MetricRegistry RatioGauge Timer Gauge)
           (java.util.concurrent TimeUnit))
  (:require [clojure.test :refer :all]
            [puppetlabs.metrics.http :refer :all]
            [schema.test :as schema-test]
            [puppetlabs.comidi :as comidi]))

(use-fixtures :once schema-test/validate-schemas)

(defn metric
  "Fetch a metric from a Dropwizard 5 registry map using a string metric name."
  [metrics-map metric-name]
  (.get metrics-map (MetricName/parse metric-name)))

(deftest test-initialize-http-metrics!
  (testing "initialize-http-metrics! should create metrics for all specified HTTP endpoints"
    (let [routes        (comidi/routes
                          (comidi/GET ["/foo/something"] request
                            "foo!")
                          (comidi/POST ["/bar" :bar] request
                            "bar!")
                          (comidi/PUT ["/baz/" :baz "/bam/" :bam] request
                            "baz!"))
          route-meta    (comidi/route-metadata routes)
          registry      (MetricRegistry.)
          http-metrics  (initialize-http-metrics! registry "localhost" route-meta)
          metrics-map   (.getMetrics registry)
          num-cpus      (metric metrics-map "puppetlabs.localhost.num-cpus")]
      (is (= #{"foo-something" "bar-:bar" "baz-:baz-bam-:bam" :other}
            (-> http-metrics :route-timers keys set)))
      (is (instance? Gauge num-cpus))
      (is (= (.availableProcessors (Runtime/getRuntime)) (.getValue num-cpus)))
      (is (instance? Timer (metric metrics-map "puppetlabs.localhost.http.other-requests")))
      (is (instance? RatioGauge (metric metrics-map "puppetlabs.localhost.http.other-percentage")))
      (is (instance? RatioGauge (metric metrics-map "puppetlabs.localhost.http.foo-something-percentage")))
      (is (instance? RatioGauge (metric metrics-map "puppetlabs.localhost.http.bar-:bar-percentage")))
      (is (instance? RatioGauge (metric metrics-map "puppetlabs.localhost.http.baz-:baz-bam-:bam-percentage"))))))

(deftest test-routes-with-same-name
  (testing "should re-use metrics objects for routes that only differ by HTTP verb"
    ;; TODO: we should consider whether or not this is the behavior we want.  It
    ;;  might be useful to have different metrics per-verb, but then our naming
    ;;  strategy will get even uglier.
    (let [routes (comidi/routes
                   (comidi/GET ["/foo" :foo] request
                     "GET foo!")
                   (comidi/PUT ["/foo" :foo] request
                     "PUT foo!"))
          route-meta (comidi/route-metadata routes)
          registry (MetricRegistry.)
          http-metrics (initialize-http-metrics! registry "localhost" route-meta)]
      (is (= #{"foo-:foo" :other}
            (-> http-metrics :route-timers keys set))))))

(deftest test-wrap-with-request-metrics
  (let [registry      (MetricRegistry.)
        routes        (comidi/routes
                        (comidi/ANY ["/foo/" :foo] request
                          "foo!"))
        route-meta    (comidi/route-metadata routes)
        http-metrics  (initialize-http-metrics! registry "localhost" route-meta)
        metrics-map   (.getMetrics registry)
        ring-app      (-> (comidi/routes->handler routes)
                        (wrap-with-request-metrics http-metrics)
                        (comidi/wrap-with-route-metadata routes))
        total-timer   (:total-timer http-metrics)
        foo-timer     (get-in http-metrics [:route-timers "foo-:foo"])
        other-timer   (get-in http-metrics [:route-timers :other])
        foo-ratio     (metric metrics-map "puppetlabs.localhost.http.foo-:foo-percentage")
        other-ratio   (metric metrics-map "puppetlabs.localhost.http.other-percentage")]
    (testing "increments endpoint timer when route matches"
      (ring-app {:uri "/foo/something"})
      (is (= 0 (.getCount other-timer)))
      (is (= 1 (.getCount foo-timer)))
      (is (= 1 (.getCount total-timer)))
      (is (= 1.0 (.. foo-ratio getRatio getValue)))
      (is (= 0.0 (.. other-ratio getRatio getValue))))
    (testing "increments :other timer when route doesn't match route-metadata"
      (ring-app {:uri "/bar/blah"})
      (is (= 1 (.getCount other-timer)))
      (is (= 1 (.getCount foo-timer)))
      (is (= 2 (.getCount total-timer)))
      (is (= 0.5 (.. foo-ratio getRatio getValue)))
      (is (= 0.5 (.. other-ratio getRatio getValue))))))

(deftest test-request-summary
  (testing "request summary returns a list sorted by aggregate time spent in requests"
    (let [registry      (MetricRegistry.)
          routes        (comidi/routes
                          (comidi/ANY ["/foo/" :foo] request
                            (do (Thread/sleep 5) "foo!"))
                          (comidi/ANY ["/bar/" :bar] request
                            (do (Thread/sleep 20) "bar!")))
          route-meta    (comidi/route-metadata routes)
          http-metrics  (initialize-http-metrics! registry "localhost" route-meta)
          ring-app      (-> (comidi/routes->handler routes)
                          (wrap-with-request-metrics http-metrics)
                          (comidi/wrap-with-route-metadata routes))]
      (ring-app {:uri "/foo/something"})
      (ring-app {:uri "/foo/something"})
      (ring-app {:uri "/bar/something"})
      (ring-app {:uri "/baz/something"})
      (let [summary (request-summary http-metrics)
            foo-summary (-> summary :routes (get "foo-:foo"))
            bar-summary (-> summary :routes (get "bar-:bar"))]
        (is (= [:total "bar-:bar" "foo-:foo" :other]
              (mapv :route-id (:sorted-routes summary))))
        (is (= 4 (-> summary :routes :total :count)))
        (is (= 2 (:count foo-summary)))
        (is (= 1 (:count bar-summary)))
        (is (= 1 (-> summary :routes :other :count)))
        (is (<= 20 (:mean bar-summary)))
        (is (<= 20 (:aggregate bar-summary)))
        (is (<= 5 (:mean foo-summary)))
        (is (<= 10 (:aggregate foo-summary)))
        (is (> (:mean bar-summary)
              (:mean foo-summary)))
        (is (> (:mean bar-summary)
              (-> summary :routes :total :mean)))))))

(deftest test-assoc-route-summary-uses-exact-sum
  (testing "aggregate is derived from Timer/getSum, not mean*count"
    (let [registry (MetricRegistry.)
          timer    (.timer registry "focused-route")]
      ;; 1ms + 2ms gives exact 3ms sum; mean truncation to millis makes mean*count differ.
      (.update timer 1 TimeUnit/MILLISECONDS)
      (.update timer 2 TimeUnit/MILLISECONDS)
      (let [summary       (#'puppetlabs.metrics.http/assoc-route-summary {} "focused-route" timer)
            route-summary (get summary "focused-route")]
        (is (= 2 (:count route-summary)))
        (is (= 3 (:aggregate route-summary)))
        (is (not= (:aggregate route-summary)
                  (* (:mean route-summary) (:count route-summary))))))))
