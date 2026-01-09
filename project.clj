(defproject org.openvoxproject/trapperkeeper-comidi-metrics "1.0.1-SNAPSHOT"
  :description "Comidi/HTTP Metrics for Trapperkeeper"
  :url "http://github.com/openvoxproject/trapperkeeper-comidi-metrics"
  :license {:name "Apache-2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0.txt"}

  :min-lein-version "2.9.1"

  :pedantic? :abort

  ;; These are to enforce consistent versions across dependencies of dependencies,
  ;; and to avoid having to define versions in multiple places. If a component
  ;; defined under :dependencies ends up causing an error due to :pedantic? :abort,
  ;; because it is a dep of a dep with a different version, move it here.
  :managed-dependencies [[org.clojure/clojure "1.12.4"]
                         [ring/ring-codec "1.1.2"]]

  :dependencies [[org.clojure/clojure]
                 [prismatic/schema "1.1.12"]
                 [org.openvoxproject/trapperkeeper-metrics "2.1.0"]
                 [org.openvoxproject/comidi "1.1.1"]]

  :deploy-repositories [["releases" {:url "https://clojars.org/repo"
                                     :username :env/CLOJARS_USERNAME
                                     :password :env/CLOJARS_PASSWORD
                                     :sign-releases false}]]

  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[org.openvoxproject/trapperkeeper "4.3.0":classifier "test"]
                                  [org.openvoxproject/kitchensink "3.5.3" :classifier "test"]
                                  [org.openvoxproject/trapperkeeper-status "1.3.0"]
                                  [org.openvoxproject/http-client "2.2.0"]
                                  [org.bouncycastle/bcpkix-jdk18on "1.83"]
                                  [org.openvoxproject/trapperkeeper-webserver-jetty10 "1.1.0"]]}}

  :aliases {"example" ["run" "-m" "example.comidi-metrics-web-app"]
            "example-data" ["run" "-m" "example.traffic-generator"]})
