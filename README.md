This project is a fork of https://github.com/ivmos/wallahack_ivmos

# wallahack
Wallapop scraper and notifier

Usage example:

```
java -jar ./build/libs/wallhack-1.0.jar -keyword "marco fotos" -limit 5 -max 500 -lat 40.41877 -long -3.69622
```

Options:

```
 -cat VAL     : Category e.g.
                        12345 (Electronics)
                        12463 (Books, Films & Music)
                        13100 (Electrical Applicances)
 -dist N      : Distance in meters from location:
                        1000 (near)
                        5000 (zone)
                        10000 (city)
                        0 (no distance)
 -keyword VAL : Keyword e.g. tele
 -lat VAL     : Latitude e.g. 41.398077
 -long VAL    : Longitude e.g. 2.170432
 -limit N     : Limit items to show e.g. 20
 -max N       : Max price e.g. 50
 -min N       : Min price e.g. 0
 -sort VAL    : Criteria to sort the results e.g.
                        creationDate-des
                        salePrice-asc
```

Compile:

```
./gradlew clean build
```
