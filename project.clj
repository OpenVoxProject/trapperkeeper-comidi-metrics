(defproject org.openvoxproject/trapperkeeper-comidi-metrics "1.0.1-SNAPSHOT"
  :description "Comidi/HTTP Metrics for Trapperkeeper"
  :url "http://github.com/openvoxproject/trapperkeeper-comidi-metrics"
  :license {:name "Apache-2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0.txt"}

  :min-lein-version "2.9.1"

  :pedantic? :abort

  ;; Generally, try to keep version pins in :managed-dependencies and the libraries
  ;; this project actually uses in :dependencies, inheriting the version from
  ;; :managed-dependencies. This prevents endless version conflicts due to deps of deps.
  ;; Renovate should keep the versions largely in sync between projects.
  :managed-dependencies [[org.clojure/clojure "1.12.4"]
                         [org.clojure/tools.logging "1.3.1"]
                         [org.clojure/tools.macro "0.2.2"]
                         [cheshire "5.13.0"]
                         [clj-time "0.15.2"]
                         [commons-codec "1.20.0"]
                         [io.dropwizard.metrics/metrics-core "3.2.6"]
                         [org.bouncycastle/bcpkix-jdk18on "1.83"]
                         [org.openvoxproject/comidi "1.1.2"]
                         [org.openvoxproject/http-client "2.2.0"]
                         [org.openvoxproject/kitchensink "3.5.5" :classifier "test"]
                         [org.openvoxproject/trapperkeeper "4.3.0" :classifier "test"]
                         [org.openvoxproject/trapperkeeper-metrics "2.1.0"]
                         [org.openvoxproject/trapperkeeper-status "1.3.0"]
                         [org.openvoxproject/trapperkeeper-webserver-jetty10 "1.1.0"]
                         [prismatic/schema "1.4.1"]
                         [ring/ring-codec "1.3.0"]]

  :dependencies [[org.clojure/clojure]
                 [org.openvoxproject/comidi]
                 [org.openvoxproject/trapperkeeper-metrics]
                 [prismatic/schema]]

  :deploy-repositories [["releases" {:url "https://clojars.org/repo"
                                     :username :env/CLOJARS_USERNAME
                                     :password :env/CLOJARS_PASSWORD
                                     :sign-releases false}]]

  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[org.bouncycastle/bcpkix-jdk18on]
                                  [org.openvoxproject/http-client]
                                  [org.openvoxproject/kitchensink :classifier "test"]
                                  [org.openvoxproject/trapperkeeper :classifier "test"]
                                  [org.openvoxproject/trapperkeeper-status]
                                  [org.openvoxproject/trapperkeeper-webserver-jetty10]]}}

  :aliases {"example" ["run" "-m" "example.comidi-metrics-web-app"]
            "example-data" ["run" "-m" "example.traffic-generator"]})
