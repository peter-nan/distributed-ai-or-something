(ns genetics-map.simulate
  (:require [genetics-map.math :as math]
            [random-seed.core :refer [rand rand-int rand-nth]]
            [clojure.string :refer [join]])

  (:refer-clojure :exclude [rand rand-int rand-nth]))

(def operations '(+ - * /))
(def mutation-rate 1.0)
(def population-size 100)

(defn set-random-seed! [seed] (random-seed.core/set-random-seed! seed))

(defn without-nth [n coll]
  (keep-indexed #(if-not (= n %1) %2) coll))

(defn nest-gene [gene] (list (rand-nth operations) gene))
(defn append-gene [gene] (concat gene (list (rand-nth '(1 x)))))

(defn remove-gene [gene]
  (let [n (rand-nth (range 1 (count gene)))]
    (without-nth n gene)))

(defn println-error [& strs]
  (.println *err* (join " " strs)))

(defn format-creature [creature]
  (str (apply list creature)))

(defn pad [n coll val]
  (take n (concat coll (repeat val))))

(defn rand-gene [genes]
  (if (empty? genes) nil (rand-nth genes)))

(defn pick-gene [& args]
  (rand-gene (remove nil? args)))

(declare mutate)
(declare possibly-mutate-gene)
(declare mutate-gene)

(defn random-list-gene [gene]
  (rand-nth [
      (append-gene gene)
      (nest-gene gene)
      (remove-gene gene)
    ]))

(defn mutate-gene [gene]
  (if (coll? gene)
    (random-list-gene gene)
    gene))

(defn mutate [gene]
  (if (> (rand) mutation-rate)
  gene
  (mutate-gene gene)))

(defn breed [population]
  (let [
      dad (pad 100 (rand-nth population) nil)
      mom (pad 100 (rand-nth population) nil)
    ]
    (mutate (remove nil? (map pick-gene dad mom)))))

(defn fitness-on-data [creature creatureFn [x y]]
    (try
      (+ (math/abs (- y (creatureFn x))) (* (count (flatten creature)) 0.0001))
      (catch ArithmeticException e Double/POSITIVE_INFINITY)
      (catch Exception e (println-error (format-creature creature)))))

(defn fitness [data creature]
  (let [creatureFn (eval (concat '(fn [x]) [creature]))]
    (math/mean (map (partial fitness-on-data creature creatureFn) data))))

(defn epoch [population data children childrenThatSurvive]
  (let [new-population (distinct (concat population (take children (repeatedly #(breed population)))))]
    (take childrenThatSurvive (sort-by (partial fitness data) new-population))))
