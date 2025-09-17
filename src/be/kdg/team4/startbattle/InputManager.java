package be.kdg.team4.startbattle;

import java.util.Scanner;

public class InputManager {
    private static final Scanner scanner = new Scanner(System.in);
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MIN_USERNAME_LENGTH = 3;
    private static String username = "";
    private static String password = "";

    public static Move cachedMove;

    public static void setUsername(String usernameInput) {
        username = usernameInput;
    }

    public static void setPassword(String passwordInput) {
        password = passwordInput;
    }


    public static Output login(){
        Output output = Output.LOGGED_IN;

        if (!DatabaseConnection.IsConnected()) {
            System.out.println("\t\tNo database connection. Skipping login.");
            return Output.LOGGED_IN;
        }

        if (!validateLoginDetails(false)) return Output.CANCEL;

        boolean loggedIn = false;
        // if the user is found
        if (DatabaseConnection.getUserID(username) != -1) {
            do {
                if (!validateLoginDetails(true)) return output;

                // Check password
                if (DatabaseConnection.checkPassword(username, password)) {
                    output = Output.LOGGED_IN;
                    loggedIn = true;
                }
                else System.out.println("\t\tPassword incorrect");
            } while (!loggedIn);
        }
        else {
            System.out.printf("\t\tUser not found, register as %s? [y/n] ", username);
            String input = scanner.nextLine().toLowerCase();
            if (input.equalsIgnoreCase("Y")) {
                do {
                    loggedIn = register();
                } while (!loggedIn);
            }
        }
        if (!loggedIn) System.out.println("\t\tLogin failed, try again.");
        else {
            System.out.println("\tLogin successful!");
        }

        return output;
    }

    private static boolean register(){
        validateLoginDetails(true);
        return DatabaseConnection.addPlayer(username, password);
    }

    private static boolean validateLoginDetails(boolean isPassword){
        String type;
        int minLength;
        if (isPassword){
            type = "Password";
            minLength = MIN_PASSWORD_LENGTH;
        }else {
            type = "Username";
            minLength = MIN_USERNAME_LENGTH;
        }


        boolean correctFormat = false;
        String input;
        do {
            System.out.printf("\tEnter your %s [min. length: %d] (type 'x' to cancel):\n\t", type, minLength);
            input = scanner.nextLine();
            if (input.length() >= minLength){
                correctFormat = true;
            }else {
                if (input.equalsIgnoreCase("X")) return false;
                System.out.printf("\t\t%s must be at least %d characters. Try again.\n", type, minLength);
            }
        }while (!correctFormat);
        if (isPassword){
            setPassword(input);
        }else {
            setUsername(input);
        }
        return true;
    }

    public static Output.GameAction validateGameInput(int size){
        Output.GameAction output = null;

        do {
            System.out.print("\tINPUT: ");
            String input = scanner.nextLine().toUpperCase();
            switch (input){
                case String s when s.equals("S") && DatabaseConnection.IsConnected() -> output = Output.GameAction.SAVE;
                case "Q" -> output = Output.GameAction.QUIT;
                default -> {
                    if (input.matches("^\\d+:\\s?\\d+$")){
                        String[] coords = input.split(":");
                        boolean outOfBounds = false;
                        for (String coord : coords) {
                            if (Integer.parseInt(coord) > size || Integer.parseInt(coord) <= 0) {
                                outOfBounds = true;
                                break;
                            }
                        }
                        if (!outOfBounds) {
                            cachedMove = new Move(
                                    Integer.parseInt(coords[0]),
                                    Integer.parseInt(coords[1])
                            );
                            output = Output.GameAction.MOVE;
                        }else {
                            System.out.println("\t\tCoordinates out of bounds! Try again.");
                        }
                    } else System.out.println("\t\tInvalid input! Try again.");
                }
            }
        }while (output == null);
        return output;
    }

    public static Output titleScreen() {
        Output output = null;
        do {
            System.out.print("\tINPUT: ");
            String input = scanner.nextLine().toUpperCase();

            switch (input) {
                case "P" -> output = Output.PLAY;
                case "L" -> output = Output.LEADERBOARD;
                case "R" -> output = Output.RULES;
                default -> System.out.println("\t\tInvalid input. Try again.");
            }
        } while (output == null);
        return output;
    }

    public static Output simpleScreen() {
        Output output = null;
        do {
            System.out.print("\tINPUT: ");
            String input = scanner.nextLine();

            if (input.equalsIgnoreCase("B")) output = Output.CANCEL;
            else System.out.println("\t\tInvalid input. Try again.");
        } while (output == null);
        return output;
    }


    public static Output.GameType chooseGame(){
        Output.GameType output = null;

        int size = 8;
        boolean validInput = false;

        do {
            System.out.println("(Only size 8 implemented!)");
            System.out.print("\tSelect board size(8-1★/10-2★/14-3★): ");

            String input = scanner.nextLine();
            switch (input) {
                case "8" -> { size = 8; validInput = true; }
                case "10" -> { size = 10; validInput = true; }
                case "14" -> { size = 14;; validInput = true; }
                default -> System.out.println("\t\tInvalid input. Try again.");
            }
        } while (!validInput);

        validInput = false;
        do {
            String input;

            if (DatabaseConnection.IsConnected()) {
                System.out.print("\tDo you want to (L)OAD or start a (N)EW game? ");
                input = scanner.nextLine();
            } else input = "N";

            if (input.equalsIgnoreCase("L")) {
                output = switch (size) {
                    case 8 -> Output.GameType.LOAD8_1;
                    case 10 -> Output.GameType.LOAD10_2;
                    case 14 -> Output.GameType.LOAD14_3;
                    default -> output;
                };
                validInput = true;
            } else if (input.equalsIgnoreCase("N")) {
                output = switch (size) {
                    case 8 -> Output.GameType.NEW8_1;
                    case 10 -> Output.GameType.NEW10_2;
                    case 14 -> Output.GameType.NEW14_3;
                    default -> output;
                };
                validInput = true;
            } else System.out.println("\t\tInvalid input. Try again.");
        } while (!validInput);
        return output;
    }

    public static boolean playAgain() {
        boolean playAgain = false;
        boolean validInput = false;

        System.out.print("\tPLAY AGAIN? (Y/N): ");

        do {
            String input = scanner.nextLine();
            if (input.equalsIgnoreCase("Y")) {
                playAgain = true;
                validInput = true;
            }
            else if (input.equalsIgnoreCase("N")) {
                playAgain = false;
                validInput = true;
            }
        } while (!validInput);

        return playAgain;
    }
}
