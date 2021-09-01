package com.simulation;

import java.util.ArrayList;
import java.util.List;
import java.io.*;

public class App 
{
    public static void main( String[] args ) throws InterruptedException {
        System.setProperty("org.graphstream.ui", "swing");

        Tools tools = new Tools();
        int number_of_games = 100;
        List<String[]>[] history_of_stats; // stores the stats for all games
        List<String[]> final_stats = new ArrayList<>(); // stores the final states averaged on all games

        history_of_stats = new List[number_of_games];
        final_stats.add(new String[] { "Turns", "Number of detected malicious nodes", "Number of false detections",
                "Total monitoring costs" });

        int total_idn = 0;

        for (int i = 0; i < number_of_games; i++) {
            //GameNotSpatial game = new GameNotSpatial();
            //GameSpatial game = new GameSpatial();
            GameSpatialOptimized game = new GameSpatialOptimized();
            //total_idn += game.get_number_IDN();
            //game.play_game();
            game.play_game_dynamic();
            System.out.println("Turn "+i);

            history_of_stats[i] = game.get_Stats();
        }

        int average_idn = total_idn/number_of_games;
        System.out.println(average_idn);

        for(int i = 0; i < history_of_stats[0].size(); i++){
            int number_detected = 0;
            int number_false_detected = 0;
            int total_monitoring_costs = 0;
            for(int j = 0; j < history_of_stats.length; j++){
                number_detected += Integer.parseInt(history_of_stats[j].get(i)[1]);
                number_false_detected += Integer.parseInt(history_of_stats[j].get(i)[2]);
                total_monitoring_costs += Integer.parseInt(history_of_stats[j].get(i)[3]);
            }
            final_stats.add(new String [] {String.valueOf(i+1),String.valueOf(number_detected/number_of_games),String.valueOf(number_false_detected/number_of_games), String.valueOf(total_monitoring_costs/number_of_games)});
        }

        try {
            tools.givenDataArray_whenConvertToCSV_thenOutputCreated(final_stats);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
