## Load library
if (!require("devtools")) {
  install.packages("devtools")
}
library(rstan)
options(mc.cores = 8)
rstan_options(auto_write = TRUE)

#devtools::install_github("b0rxa/scmamp", force=TRUE)

getCredibleIntervalsWeights <- function(posterior.samples, interval.size=0.9) {
  qmin <- (1-interval.size)/2
  qmax <- 1-qmin
  lower.bound <- apply(posterior.samples, MARGIN=2, FUN=quantile, p=qmin) 
  upper.bound <- apply(posterior.samples, MARGIN=2, FUN=quantile, p=qmax) 
  expectation <- apply(posterior.samples, MARGIN=2, FUN=mean) 
  return (data.frame(Expected=expectation, Lower_bound=lower.bound, Upper_bound=upper.bound))
}
library("scmamp")

library("ggplot2")
setwd("/Users/isaaclozano/Documents/GitHub/TFG-Rank-Pricing-Problem/Ranking_Bayesiano_algoritmos")
df <- read.csv("datos.csv")

## Statistical Analysis
df[,2:length(df)] <- df[,2:length(df)] * -1
pl_model <-  bPlackettLuceModel(x.matrix=df[,2:length(df)], min=TRUE, nsim=4000, nchains=20, parallel=TRUE)
pl_model$expected.mode.rank
pl_model$expected.win.prob


alg_names = c("CMSA","IG")


## Plot
processed.results <- getCredibleIntervalsWeights(pl_model$posterior.weights, interval.size=0.9)
df <- data.frame(Algorithm=rownames(processed.results), processed.results)
ggplot(df, aes(y=Expected, ymin=Lower_bound, ymax=Upper_bound, x=factor(Algorithm,alg_names))) + geom_errorbar() + geom_point(col="darkgreen", size=2) +  theme_bw() + coord_flip() + labs(y="Probability of winning") + labs(x="Algorithm")

