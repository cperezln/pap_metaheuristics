## Load library
if (!require("devtools")) {
  install.packages("devtools")
}
options(mc.cores = 8)
rstan_options(auto_write = TRUE)
devtools::install_github("b0rxa/scmamp", force=TRUE)

getCredibleIntervalsWeights <- function(posterior.samples, interval.size=0.9) {
  qmin <- (1-interval.size)/2
  qmax <- 1-qmin
  lower.bound <- apply(posterior.samples, MARGIN=2, FUN=quantile, p=qmin) 
  upper.bound <- apply(posterior.samples, MARGIN=2, FUN=quantile, p=qmax) 
  expectation <- apply(posterior.samples, MARGIN=2, FUN=mean) 
  return (data.frame(Expected=expectation, Lower_bound=lower.bound, Upper_bound=upper.bound))
}
library("scmamp")

## Load data
setwd("/Users/aherrang/OneDrive - Universidad Rey Juan Carlos/Work/Ejecuciones/CLUSTERING/MLCP/results/tests")
library("readxl")
df <- read_excel("test_avg.xlsx")

## Statistical Analysis
#df[,2:length(df)] <- df[,2:length(df)] * -1
df[,2:length(df)] <- df[,2:length(df)]
pl_model <-  bPlackettLuceModel(x.matrix=df[,2:length(df)], min=FALSE, nsim=4000, nchains=20, parallel=TRUE)
pl_model$expected.mode.rank
pl_model$expected.win.prob

## Plot
library("ggplot2")
processed.results <- getCredibleIntervalsWeights(pl_model$posterior.weights, interval.size=0.9)
df <- data.frame(Algorithm=rownames(processed.results), processed.results)
ggplot(df, aes(y=Expected, ymin=Lower_bound, ymax=Upper_bound, x=Algorithm)) + geom_errorbar() + geom_point(col="darkgreen", size=2) +  theme_bw() + coord_flip() + labs(y="Probability of winning")

