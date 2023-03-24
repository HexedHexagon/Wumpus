/*
 *   __      ___   _ __  __ ___ _   _ ___ 
 *  \ \    / / | | |  \/  | _ \ | | / __|
 *   \ \/\/ /| |_| | |\/| |  _/ |_| \__ \
 *    \_/\_/  \___/|_|  |_|_|  \___/|___/
 *   
 * Made as my first 'larger' java project at school when we were learning about arrays back in February '23,
 * before we were introduced to object-oriented programming principles, so it's all one class. 
 * 
 * I figured many things out as I went, so some functions look cleaner than others.
 * The highscore list is a work in progress. I am not quite happy with the file handling via file path yet as I
 * would like to make this game into an actual functioning executable once I figure out deployment, and the file
 * path will inevitably mess that up. At least it works on my machine atm! :o)
 * 
 * NOTE: when I was half-done I realised that setting the labyrinth up as a (2-dimensional) int-array instead of
 * a String array might have saved me a lot of parsing. I am not going to rework that for now, but updates to
 * Wumpus arent't completely out of the question.
 * 
 * Game made by Fee / https://github.com/HexedHexagon
 * 
 * ASCII art from https://patorjk.com/software/taag/
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

class Wumpus
{
    static Scanner input = new Scanner(System.in);
    static int language;
    static boolean exit = false;

    // following are all ingame texts that are needed, in multiple (atm 2) languages.
    // index [x][0] - English
    // index [x][1] - German
    static String[] playerToken = {"AD", "AB"},
                    batToken = {"BT", "FL"},
                    smellsLikeWumpusMsg = {"It smells like WUMPUS in this spot!", "Es riecht hier nach WUMPUS!"},
                    moveOrThrowMsg = {"Would you like to move (w,a,s,d) or throw your spear (t)?", 
                                      "Moechtest du dich bewegen (w,a,s,d) oder den Speer werfen (t)?"},
                    spearThrowDirectionMsg = {"Which direction do you want to throw the spear (w,a,s,d)?", 
                                              "In welche Richtung moechtest du den Speer werfen (w,a,s,d)?"},
                    moveDirectionMsg = {"Where would you like to move (w,a,s,d)?", "In welche Richtung moechtest du laufen?"},
                    wumpusMoveMsg = {"WUMPUS moved!", "WUMPUS hat sich bewegt!"},
                    spearThrowMsg = {"You threw the spear.", "Du hast den Speer geworfen."},
                    invalidInputMsg = {"Invalid input! Please try again.", "Ungueltige Eingabe! Bitte versuch es nochmal."},
                    wallMsg = {"There's a wall there! Please choose a different direction.", 
                               "Da ist eine Wand! Bitte waehle eine andere Richtung."},
                    batMsg = {"You are being carried away by giant bats!", 
                              "Du wirst von Riesenfledermaeusen fortgetragen!"},
                    gameLoseMsg = {"You got eaten by WUMPUS!\nGAME OVER", "WUMPUS hat dich gefressen!\nGAME OVER"},
                    gameWinMsg = {"You have killed WUMPUS!\nYOU WON", "Du hast WUMPUS besiegt!\nSPIEL GEWONNEN"},
                    menu = {"WUMPUS\n\t0: START GAME\n\t1: HOW TO PLAY\n\t2: SHOW LIST OF WUMPUS-SLAYERS\n\t3: CHANGE LANGUAGE\n\t4: EXIT",
                            "WUMPUS\n\t0: SPIEL STARTEN\n\t1: SPIELANLEITUNG\n\t2: LISTE DER WUMPUSBEZWINGER\n\t3: SPRACHE AENDERN\n\t4: BEENDEN"},
                    instructions = {"\nWelcome, adventurer!\nTread carefully in these halls, the WUMPUS might be lurking " +
                                    "behind every corner.\nGiant bats will carry you away when you disturb their slumber." +
                                    "\nYou cannot see the WUMPUS, but you can smell it when it's near..." +
                                    "\nHold onto your spear and go slay the WUMPUS!\n" +
                                    "\nWW = WUMPUS    BT = Bat    AD = Adventurer    SP = Spear\n",
                                    "\nWillkommen, Abenteurer!\nHab Acht, denn der WUMPUS koennte hinter jeder " +
                                    "Ecke auf dich lauern.\nRiesige Fledermaeuse schnappen dich und tragen dich hinfort, wenn du ihren Schlaf stoerst." +
                                    "\nDu kannst den WUMPUS nicht sehen, aber du kannst ihn riechen, wenn du ihm nahe genug bist..." +
                                    "\nNun geh los und jag den WUMPUS!\n" +
                                    "\nWW = WUMPUS    FL = Fledermaus    AB = Abenteurer    SP = Speer\n"},
                    highscoreMenuHeader = {"SLAYERS OF THE WUMPUS\n    ACTIONS  NAME\n", "BEZWINGER DES WUMPUS\n  AKTIONEN  NAME\n"},
                    getSlayerMsg = {"Please enter your name:", "Bitte gib deinen Namen ein:"},
                    slayerAddedMsg = {"A new slayer was added to the list!", "Ein neuer Bezwinger wurde der Liste hinzugefuegt!"};

    public static void main (String[] args) 
    {   
        // Initial Language Setting
        language = getLanguage();
        
        // Start Menu
        do
        {
            System.out.println(menu[language]);
            switch (getMenuOption())
            {
                case '0':
                    playGame();
                    break;
                case '1':
                    System.out.println(instructions[language]);
                    break;
                case '2':
                    highscore();
                    break;
                case '3':
                    language = getLanguage();
                    break;
                case '4':
                    exit = true;
                    break;
            }
        } while (!exit);        
        input.close();
    }

    private static void playGame ()
    {
        // Initialise game
        String[][] labyrinth = getLabyrinth();
        generateBats(labyrinth);
        int[] wumpusPosition = generateWumpus(labyrinth);
        boolean gameOver = false;
        char action = ' ';
        int[] playerPosition = generatePlayerPos(labyrinth, true, true);
        int actionCounter = 0;
                
        // GAME ON PEW PEW PEW
        while (gameOver == false)
        {
            actionCounter++;

            // show labyrinth and check for wumpus smell
            printLabyrinth(labyrinth, false);
            if (wumpusNearby(labyrinth, playerPosition))
                System.out.println(smellsLikeWumpusMsg[language]);

            // START player actions
            if (hasSpear(labyrinth, playerPosition))
            {
                System.out.println(moveOrThrowMsg[language]);
                action = moveOrThrow(labyrinth, playerPosition);
                if (action == 't')
                {
                    System.out.println(spearThrowDirectionMsg[language]);
                    throwSpear(labyrinth, getDirection(labyrinth, playerPosition), playerPosition);
                    if (hasSpear(labyrinth, wumpusPosition))
                        {
                            printLabyrinth(labyrinth, true);
                            System.out.println(gameWinMsg[language]);
                            gameOver = true;
                            addSlayer(actionCounter);
                        }
                    else
                        wumpusPosition = moveWumpus(labyrinth, wumpusPosition);
                }
                else
                    playerPosition = movePlayer(labyrinth, playerPosition, action, true);
            }
            else
            {
                System.out.println(moveDirectionMsg[language]);
                playerPosition = movePlayer(labyrinth, playerPosition, getDirection(labyrinth, playerPosition), false);
            }
            // END player actions

            // check for bats(-> teleport) and wumpus(-> player gets eaten, GAME OVER)
            if (hasBat(labyrinth, playerPosition))
            {
                System.out.println(batMsg[language]);
                playerPosition = generatePlayerPos(labyrinth, hasSpear(labyrinth, playerPosition), false, playerPosition);
                wumpusPosition = moveWumpus(labyrinth, wumpusPosition);
            }
            if (hasWumpus(labyrinth, playerPosition))
            {
                printLabyrinth(labyrinth, true);
                System.out.println(gameLoseMsg[language]);
                gameOver = true;
            }
        }
    }
    
    private static void printLabyrinth (String[][] labyrinth, boolean showAll)
    {
        // every line is iterated twice because one field on the labyrinth map is two lines high (and four chars wide)
        // +----+
        // |WWAD| WW - WUMPUS    AD - ADVENTURER
        // |BTSP| BT - BAT       SP - SPEAR 
        // +----+
        //
        // +----+
        // |XXXX| Wall
        // |XXXX|
        // +----+
        int[] positions = new int[2];
        for (int i = 0; i < labyrinth.length; i++)
        {
            positions[0] = i;
            for (int k = 0; k < 2; k++)
            {
                for (int j = 0; j < labyrinth[i].length; j++)
                {
                    positions[1] = j;
                    if (hasWall(labyrinth, positions))
                        System.out.print("XXXX");
                    else if (k == 0)
                    {
                        if (showAll && hasWumpus(labyrinth, positions))
                            System.out.print("WW");
                        else
                            System.out.print("  ");

                        if (hasPlayer(labyrinth, positions))
                            System.out.print(playerToken[language]);
                        else
                            System.out.print("  ");
                    }
                    else
                    {
                        if (showAll && hasBat(labyrinth, positions))
                            System.out.print(batToken[language]);
                        else
                            System.out.print("  ");

                        if (hasSpear(labyrinth, positions))
                            System.out.print("SP");
                        else
                            System.out.print("  ");
                    }
                }
                System.out.print('\n');
            }
        }
    }

    private static void generateBats (String[][] labyrinth)
    {
        // sets 3 bats to random empty positions on the map 
        boolean setBat;
        int     batPositionX,
                batPositionY;
        int[] positions = new int[2];
        for (int i = 0; i < 3; i++)
        {
            setBat = false;
            while (!setBat)
            {
                positions[1] = batPositionX = (int)(Math.random() * (labyrinth[0].length - 2) + 1);
                positions[0] = batPositionY = (int)(Math.random() * (labyrinth.length - 2) + 1);
                if (!hasWall(labyrinth, positions) &&
                    !hasBat(labyrinth, positions))
                {
                    labyrinth[batPositionY][batPositionX] = "00100";
                    setBat = true;
                }
            }
        }
    }

    private static int[] generateWumpus (String[][] labyrinth)
    {
        // sets wumpus position to a random spot on the map that does not have a wall (but might have a bat)
        boolean setWumpus = false;
        int wumpusPositionX,
            wumpusPositionY;
        int[] positions = new int[2];
        while (!setWumpus)
        {
            positions[1] = wumpusPositionX = (int)(Math.random() * (labyrinth[0].length - 2) + 1);
            positions[0] = wumpusPositionY = (int)(Math.random() * (labyrinth.length - 2) + 1);
            if (!hasWall(labyrinth, positions))
            {
                labyrinth[wumpusPositionY][wumpusPositionX] = nullCheck(Integer.toString(Integer.parseInt(labyrinth[wumpusPositionY][wumpusPositionX]) + 1000));
                setWumpus = true;
            }
        }
        return positions;
    }

    private static int[] generatePlayerPos (String[][] labyrinth, boolean hasSpear, boolean initialSpawn, int ... formerPosition)
    {
        // both used for putting the player on their random initial starting position    (when initialSpawn == true)
        // and for teleporting the player to a new random spot when carried away by bats (when initialSpawn == false)
        boolean setPos = false;
        int PositionX = 0,
            PositionY = 0;
        int[] positions = new int[2];
        while (!setPos)
        {
                PositionX = (int)(Math.random() * (labyrinth[0].length - 2) + 1);
                PositionY = (int)(Math.random() * (labyrinth.length - 2) + 1);
                positions[0] = PositionY;
                positions[1] = PositionX;
                if (!hasWall(labyrinth, positions))
                {
                    labyrinth[PositionY][PositionX] = nullCheck(Integer.toString(Integer.parseInt(labyrinth[PositionY][PositionX]) + (hasSpear ? 11 : 10)));
                    setPos = true;
                }
        }
        if (!initialSpawn)
        {
            // if this is used for the bat teleport, the player (and spear if with the player) has to be removed from their former position
            labyrinth[formerPosition[0]][formerPosition[1]] = nullCheck(Integer.toString(Integer.parseInt(labyrinth[formerPosition[0]][formerPosition[1]]) - (hasSpear ? 11 : 10)));
        }
        return positions;
    }

    private static int[] movePlayer (String[][] labyrinth, int[] playerPosition, char direction, boolean withSpear)
    {
        // moves the player and, if they have it, the spear, one field in the specified direction
        int[] positions = new int[2];
        int additive = (withSpear ? 11 : 10);
        switch (direction)
        {
            case 'w':
            {
                // up
                positions[0] = playerPosition[0] - 1;
                positions[1] = playerPosition[1];
                break;
            }
            case 'a':
            {
                // left
                positions[0] = playerPosition[0];
                positions[1] = playerPosition[1] - 1;
                break;
            }
            case 's':
            {
                // down
                positions[0] = playerPosition[0] + 1;
                positions[1] = playerPosition[1];
                break;
            }
            case 'd':
            {
                // right
                positions[0] = playerPosition[0];
                positions[1] = playerPosition[1] + 1;
                break;
            }
            default:
                break;
        }
        // add player / spear to new location
        labyrinth[positions[0]][positions[1]] = nullCheck(Integer.toString(additive + Integer.parseInt(labyrinth[positions[0]][positions[1]])));
        // remove player / spear from current location
        labyrinth[playerPosition[0]][playerPosition[1]] = nullCheck(Integer.toString(-additive + Integer.parseInt(labyrinth[playerPosition[0]][playerPosition[1]])));
        return positions;
    }

    public static int[] moveWumpus (String[][] labyrinth, int[] wumpusPosition)
    {
        // moves the wumpus one field in a random direction
        // used when he wakes up from a spear being thrown or bats moving the player
        boolean setPos = false;
        int positionX = 0,
            positionY = 0;
        int[] positions = new int[2];
        char direction;
        String directions = "wasd";
        while (!setPos)
        {
            direction = directions.charAt((int)(Math.random() * 4));
            switch (direction)
            {
                case 'w':
                    positionY = wumpusPosition[0] - 1;
                    positionX = wumpusPosition[1];
                    break;
                case 'a':
                    positionY = wumpusPosition[0];
                    positionX = wumpusPosition[1] - 1;
                    break;
                case 's':
                    positionY = wumpusPosition[0] + 1;
                    positionX = wumpusPosition[1];
                    break;
                case 'd':
                    positionY = wumpusPosition[0];
                    positionX = wumpusPosition[1] + 1;
                    break;
            }
            positions[0] = positionY;
            positions[1] = positionX;
            if (!hasWall(labyrinth, positions))
            {
                labyrinth[positionY][positionX] = nullCheck(Integer.toString(Integer.parseInt(labyrinth[positionY][positionX]) + 1000));
                labyrinth[wumpusPosition[0]][wumpusPosition[1]] = nullCheck(Integer.toString(Integer.parseInt(labyrinth[wumpusPosition[0]][wumpusPosition[1]]) - 1000));
                System.out.println(wumpusMoveMsg[language]);
                setPos = true;
            }
        }
        return positions;
    }

    private static void throwSpear (String[][] labyrinth, char direction, int[] playerPosition)
    {
        int[] target = {playerPosition[0], playerPosition[1]};
        switch (direction)
        {
            case 'w':
                target[0]--;
                break;
            case 'a':
                target[1]--;
                break;
            case 's':
                target[0]++;
                break;
            case 'd':
                target[1]++;
                break;
        }
        // add spear to new position
        labyrinth[target[0]][target[1]] = nullCheck(Integer.toString((Integer.parseInt(labyrinth[target[0]][target[1]])) + 1));
        System.out.println(spearThrowMsg[language]);
        // remove spear from old position
        labyrinth[playerPosition[0]][playerPosition[1]] = nullCheck(Integer.toString((Integer.parseInt(labyrinth[playerPosition[0]][playerPosition[1]]) - 1)));
    }

    private static boolean wumpusNearby (String[][] labyrinth, int[] playerPosition)
    {
        // checks if the wumpus is on a field directly adjacent to the player (not diagonally!)
        int[] w = {playerPosition[0] - 1, playerPosition[1]},
              a = {playerPosition[0], playerPosition[1] - 1},
              s = {playerPosition[0] + 1, playerPosition[1]},
              d = {playerPosition[0], playerPosition[1] + 1};
        if (hasWumpus(labyrinth, w) ||
            hasWumpus(labyrinth, a) ||
            hasWumpus(labyrinth, s) ||
            hasWumpus(labyrinth, d))
            return true;
        else
            return false;
    }

    private static String nullCheck (String coordinate)
    {
        // makes sure the Strings that stand for the contents of a certain field in the labyrinth String array
        // have correct leading 0s when needed, as parsing from String to int and to String again tends to mess these up...
        // the thought that I could've used an int array instead keeps me up at night sometimes.
        int len = coordinate.length();
        
        for (int i = 0; i < 5 - len; i++)
        {
            coordinate = '0' + coordinate;
        }
        return coordinate;
    }

    private static boolean hasSpear (String[][] labyrinth, int[] position)
    {
        if (labyrinth[position[0]][position[1]].charAt(4) == '1')
            return true;
        else
            return false;
    }

    private static boolean hasWall (String[][] labyrinth, int[] position)
    {
        if (labyrinth[position[0]][position[1]].charAt(0) == '1')
            return true;
        else
            return false;
    }

    private static boolean hasWall (String[][] labyrinth, int[] position, char direction)
    {
        int[] w = {position[0] - 1, position[1]},
              a = {position[0], position[1] - 1},
              s = {position[0] + 1, position[1]},
              d = {position[0], position[1] + 1};
        switch (direction)
        {
            case 'w':
                if (hasWall(labyrinth, w))
                    return true;
                else
                    return false;
            case 'a':
                if (hasWall(labyrinth, a))
                    return true;
                else
                    return false;
            case 's':
                if (hasWall(labyrinth, s))
                    return true;
                else
                    return false;
            case 'd':
                if (hasWall(labyrinth, d))
                    return true;
                else
                    return false;
            default:
                return true;
        }
    }

    private static boolean hasWumpus (String[][] labyrinth, int[] position)
    {
        if (labyrinth[position[0]][position[1]].charAt(1) == '1')
            return true;
        else
            return false;
    }

    private static boolean hasBat (String[][] labyrinth, int[] position)
    {
        if (labyrinth[position[0]][position[1]].charAt(2) == '1')
            return true;
        else
            return false;
    }

    private static boolean hasPlayer (String[][] labyrinth, int[] position)
    {
        if (labyrinth[position[0]][position[1]].charAt(3) == '1')
            return true;
        else
            return false;
    }

    private static char moveOrThrow (String[][] labyrinth, int[] position)
    {
        // asked during player action phase if the player has the spear
        // returns: t (throw) | w (move up) | a (move left) | s (move down) | d (move right)
        boolean valid = false;
        String userIn = "",
               allowedInputs = "wasdt";
        
        while (!valid)
        {
            userIn = input.nextLine();
            if (userIn.length() == 1 && allowedInputs.contains(userIn.toLowerCase()))               
            {
                if (userIn.charAt(0) == 't' || !hasWall(labyrinth, position, userIn.charAt(0)))
                    valid = true;
                else
                    System.out.println(wallMsg[language]);
            }
            else
                System.out.println(invalidInputMsg[language]);
        }
        return userIn.charAt(0);
    }

    private static char getDirection (String[][] labyrinth, int[] position)
    {
        // for moving and throwing
        // returns: w (up) | a (left) | s (down) | d (right)
        boolean valid = false;
        String userIn = "",
               allowedInputs = "wasd";
        
        while (!valid)
        {
            userIn = input.nextLine();
            if (userIn.length() == 1 && allowedInputs.contains(userIn.toLowerCase()))               
            {
                if (!hasWall(labyrinth, position, userIn.charAt(0)))
                    valid = true;
                else
                    System.out.println(wallMsg[language]);
            }
            else
                System.out.println(invalidInputMsg[language]);
        }
        return userIn.charAt(0);
    }

    private static int getLanguage ()
    {
        boolean valid = false;
        String userIn = "",
               allowedInputs = "01";
        
        while (!valid)
        {
            System.out.println("Choose your language:\n" +
                           "0: English\n" +
                           "1: Deutsch");
            userIn = input.nextLine();
            if (userIn.length() == 1 && allowedInputs.contains(userIn))               
            {
                valid = true;
            }
            else
                System.out.println("Invalid input!");
        }
        return Character.getNumericValue(userIn.charAt(0));
    }

    private static char getMenuOption ()
    {
        boolean valid = false;
        String userIn = "",
               allowedInputs = "01234";
        
        while (!valid)
        {
            userIn = input.nextLine();
            if (userIn.length() == 1 && allowedInputs.contains(userIn))               
            {
                valid = true;
            }
            else
                System.out.println(invalidInputMsg[language] + '\n');
        }
        return (userIn.charAt(0));
    }

    private static String[][] getLabyrinth ()
    {
        String[][][] labyrinths = 
        {
        {{"10000", "10000", "10000", "10000", "10000", "10000", "10000", "10000", "10000", "10000", "10000", "10000"},
        {"10000", "00000", "00000", "00000", "00000", "00000", "00000", "00000", "00000", "00000", "00000", "10000"},
        {"10000", "00000", "00000", "00000", "00000", "00000", "00000", "00000", "00000", "00000", "00000", "10000"},
        {"10000", "00000", "00000", "10000", "10000", "10000", "10000", "10000", "10000", "00000", "00000", "10000"},
        {"10000", "00000", "00000", "10000", "00000", "00000", "00000", "00000", "00000", "00000", "00000", "10000"},
        {"10000", "00000", "00000", "10000", "00000", "00000", "00000", "00000", "00000", "00000", "00000", "10000"},
        {"10000", "00000", "00000", "00000", "00000", "00000", "00000", "00000", "10000", "00000", "00000", "10000"},
        {"10000", "00000", "00000", "00000", "00000", "00000", "00000", "00000", "10000", "00000", "00000", "10000"},
        {"10000", "00000", "00000", "10000", "10000", "10000", "10000", "10000", "10000", "00000", "00000", "10000"},
        {"10000", "00000", "00000", "00000", "00000", "00000", "00000", "00000", "00000", "00000", "00000", "10000"},
        {"10000", "00000", "00000", "00000", "00000", "00000", "00000", "00000", "00000", "00000", "00000", "10000"},
        {"10000", "10000", "10000", "10000", "10000", "10000", "10000", "10000", "10000", "10000", "10000", "10000"}},

        {{"10000", "10000", "10000", "10000", "10000", "10000", "10000", "10000", "10000", "10000", "10000", "10000"},
        {"10000", "00000", "00000", "10000", "00000", "00000", "00000", "00000", "10000", "00000", "00000", "10000"},
        {"10000", "00000", "00000", "10000", "00000", "00000", "00000", "00000", "10000", "00000", "00000", "10000"},
        {"10000", "00000", "00000", "10000", "00000", "10000", "00000", "00000", "00000", "00000", "00000", "10000"},
        {"10000", "00000", "00000", "10000", "00000", "00000", "10000", "00000", "00000", "00000", "00000", "10000"},
        {"10000", "00000", "00000", "10000", "00000", "00000", "00000", "10000", "10000", "00000", "00000", "10000"},
        {"10000", "00000", "00000", "10000", "10000", "00000", "00000", "00000", "10000", "00000", "00000", "10000"},
        {"10000", "00000", "00000", "00000", "00000", "10000", "00000", "00000", "10000", "00000", "00000", "10000"},
        {"10000", "00000", "00000", "00000", "00000", "00000", "10000", "00000", "10000", "00000", "00000", "10000"},
        {"10000", "00000", "00000", "10000", "00000", "00000", "00000", "00000", "10000", "00000", "00000", "10000"},
        {"10000", "00000", "00000", "10000", "00000", "00000", "00000", "00000", "10000", "00000", "00000", "10000"},
        {"10000", "10000", "10000", "10000", "10000", "10000", "10000", "10000", "10000", "10000", "10000", "10000"}},

        {{"10000", "10000", "10000", "10000", "10000", "10000", "10000", "10000", "10000", "10000", "10000", "10000"},
        {"10000", "00000", "00000", "00000", "00000", "00000", "10000", "00000", "00000", "00000", "00000", "10000"},
        {"10000", "00000", "00000", "00000", "00000", "00000", "10000", "00000", "00000", "00000", "00000", "10000"},
        {"10000", "00000", "00000", "10000", "00000", "00000", "10000", "00000", "00000", "00000", "00000", "10000"},
        {"10000", "00000", "00000", "00000", "00000", "00000", "00000", "00000", "00000", "00000", "00000", "10000"},
        {"10000", "00000", "00000", "00000", "00000", "00000", "00000", "00000", "10000", "10000", "10000", "10000"},
        {"10000", "10000", "10000", "10000", "00000", "00000", "00000", "00000", "00000", "00000", "00000", "10000"},
        {"10000", "00000", "00000", "00000", "00000", "00000", "00000", "00000", "00000", "00000", "00000", "10000"},
        {"10000", "00000", "00000", "00000", "00000", "10000", "00000", "00000", "10000", "00000", "00000", "10000"},
        {"10000", "00000", "00000", "00000", "00000", "10000", "00000", "00000", "00000", "00000", "00000", "10000"},
        {"10000", "00000", "00000", "00000", "00000", "10000", "00000", "00000", "00000", "00000", "00000", "10000"},
        {"10000", "10000", "10000", "10000", "10000", "10000", "10000", "10000", "10000", "10000", "10000", "10000"}}
        };

        return labyrinths[(int)(Math.random() * labyrinths.length)];
    }

    // NOTE: highscore list functions will have bugs if you take more than 999 actions to slay the WUMPUS
    // (which frankly shouldn't ever happen)
    public static void highscore ()
    {
        // initiate file, reader, arraylist for storing file's content and list reader
        File slayerList = null;
        Scanner fileReader = null;
        ArrayList<String> fileContent = new ArrayList<String>();
        try 
        {
            slayerList = new File("schule/26_Wumpus/src/slayers.txt");
            slayerList.createNewFile();
            fileReader = new Scanner(slayerList);         
        } 
        catch (IOException e) 
        {
            System.out.println("An error occurred :(");
            e.printStackTrace();
        }

        // write content of file to arraylist
        while (fileReader.hasNext())
            fileContent.add(fileReader.nextLine());
        fileReader.close();

        // sort arraylist by number of actions (ascending)
        Collections.sort(fileContent);

        // print highscore list
        System.out.print('\n' + highscoreMenuHeader[language]);
        for (int i = 0; i < fileContent.size(); i++)
            System.out.println("\t" + fileContent.get(i));
    }

    public static void addSlayer (int actions)
    {
        // initiate file and writer and actions to String
        String actionsStr = Integer.toString(actions);
        File slayerList = null;
        FileWriter fileWriter = null;
        try 
        {
            slayerList = new File("schule/26_Wumpus/src/slayers.txt");
            slayerList.createNewFile();
            fileWriter = new FileWriter(slayerList, true);
        } 
        catch (IOException e) 
        {
            System.out.println("An error occurred :(");
            e.printStackTrace();
        }

        // get new slayer's name
        boolean valid = false;
        String userIn = "";
        System.out.println(getSlayerMsg[language]);     
        while (!valid)
        {
            userIn = input.nextLine();
            if (userIn.length() > 0)               
            {
                valid = true;
            }
            else
                System.out.println(invalidInputMsg[language] + '\n');
        }

        // write new name to file
        try
        {
            fileWriter.write("\n" + (actionsStr.length() == 1 ? "00" : actionsStr.length() == 2 ? "0" : "") + actions + "  " + userIn);
            fileWriter.close();
        }
        catch (IOException e) 
        {
            System.out.println("An error occurred :(");
            e.printStackTrace();
        }
        System.out.println(slayerAddedMsg[language]);
    }
}