(ns metrajay.duckdb)

(defn get-duckdb []
  (.-DuckDB js/window))

(defn get-db-conn []
  (.-dbConnection js/window))

(defn query-duckdb [sql]
  (let [conn (get-db-conn)]
    (when conn
      (.then
       (.query conn sql)
       (fn [res]
         (let [rows (.toArray res)
               json-rows (.map rows (fn [x] (.toJSON x)))]
           (js->clj json-rows {:keywordize-keys true})))))))
