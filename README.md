To setup, the javarosa and commcare projects must be in the same top level folder as this project

To run:
./gradlew build; java -jar build/libs/formsplayer


fswatch -o ~/Dimagi/commcare-spring | xargs -n1 /Users/willpride/scripts/build_spring.sh