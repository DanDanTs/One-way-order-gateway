# One-way-order-gateway
Dummy order gateway which listens to a UDP Port and forward the same message to TCP Server


## Dev Environment:
This project is built as Maven project with `Apache NetBeans 12.6` on `JDK 8` under Windows.

## Steps:
1. Download or git clone the project to local repo
2. Run the testing `Python script` for TCP server
3. Execute the `run.sh` to start the Order Gateway
4. Type key `q` and `Enter` to quit the application


## Tuning
Garbage Collector `ParallelGC` has been chosen to speed up garbage collection.
And, `GCTimeRatio` set to `5` to have approximately 16.67% of total time in 
garbage collection.


## Result for latency test
Testing Env: Windows 10, 6 Core 12 Thread, 8 GB Ram
Throttle = 100
```sh
avg = 69597.71, 0 = 0, 50 = 87000, 95 = 135800, 99 = 163900, 100 = 1170500
```

After the testing environment reboot, the result went like:
```sh
avg = 9002.8, 0 = 0, 50 = 0, 95 = 0, 99 = 0, 100 = 28983500
```

## Further enhancement
`ConcurrentLinkedQueue` in `MessageBuffer` should be completely removed.