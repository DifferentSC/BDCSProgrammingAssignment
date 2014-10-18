# Overview

This is an implementation of L-BFGS for linear regression on REEF. It finds an linear model like `a0 + a1 * x1 + a2 * x2 + a3 * x3 + ...`, which minizes the loss function. It spreads data across the worker nodes, calculates gradients, and summates those gradients to get overall gradients. Other than that, each nodes' operations are all local.

# Usage

You can excute the MLPracticeClient by putting `./bin/run.sh [command args]` in shell. You should be located at your homefolder when you are executing the command.

## Commandline arguments
* -iters (default = 10): maximum number of iterations
* -workers (default = 3): number of worker tasks
* -lambda (default = 0.001): degree or regularization
* -input (default = /input.csv): location of input file on HDFS

# Input/Output

It reads input from Hadoop file system and it's address is fixed to "hdfs://localhost:9000". After execution, it produces output to HDFS and it's stored at "hdfs://localhost:9000/output.txt". The output file has information about parameter vector and error value on each iteration.

I attached the training-set I used on my repo. It's from UCI Machine Learning Repository (https://archive.ics.uci.edu/ml/datasets.html)
