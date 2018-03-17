# YoBit.Net trading bot
Experimental trading bot for YoBit.Net.
You can use it at your own risk.

## Features
- Parallel trading in several pairs
- Parallel trading within each pair (by default 3 parallel trades)
- Auto selecting pairs for trading (by default every 30 min)
- Configuring preferred or unwanted pairs
- Tuning thresholds and other params (max buy sum, min sell profit, stoploss rate, etc.)
- Output information to console

## Run
1. Find a config file *application.yaml* and put your keys into params *key* and *secretKey*:
```
    key: <YOUR_KEY>
    secretKey: <YOUR_SECRET_KEY>
```
2. Run a command at the root of the directory:
```
mvn spring-boot:run
```