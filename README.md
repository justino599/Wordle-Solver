# Wordle-Solver
This was a project I worked on for a bit that is a Wordle game, but more importantly, a Wordle solver. It can be played as a regular game of wordle if you set `autoPlay` to false in the main method of the `Game` class, but if you leave as true, it will continually find the best guess to make and play that move. With this algorithm, I have achieved a win streak of 1156 and there is definitely still room for improvement as demonstated by 3Blue1Brown in his two Wordle videos ["Solving Wordle using information theory"](https://youtu.be/v68zYyaEmEA) and ["Oh, wait, actually the best Wordle opener is not “crane”…"](https://youtu.be/fRed0Xmc2Wg). These two videos inspired me to do this project, but I tried to do it in my own way so that I would learn more, because learning is fun!

## Jar releases
I've compiled a few jars with different parameters set if you want to run it without having to compile the project yourself. It will require having Java 8 or higher installed. If you run them from the command line using `java -jar Wordle-1.x.jar`, it will output all of the possible guesses as well as what it thinks is the best guess to the terminal. 
- [Stand-alone Wordle game](https://github.com/justino599/Wordle-Solver/releases/tag/1.0)
- [Bot auto plays the game, at a readable speed](https://github.com/justino599/Wordle-Solver/releases/tag/1.1)
- [Bot plays the game at max speed](https://github.com/justino599/Wordle-Solver/releases/tag/1.2)
