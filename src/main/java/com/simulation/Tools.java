package com.simulation;

import java.util.stream.*;
import java.io.*;
import java.util.List;

public class Tools {

    public Tools(){

    }

    // Adds a node to an array of nodes
    public Node[] add_node(Node[] table, Node node) {
        Node[] result = new Node[table.length + 1];

        for (int i = 0; i < table.length; i++) {
            result[i] = table[i];
        }
        result[table.length] = node;

        return result;
    }

    public int[] add_id(int[] table, int id){
        int[] result = new int[table.length + 1];

        for (int i = 0; i < table.length; i++) {
            result[i] = table[i];
        }
        result[table.length] = id;

        return result;
    }

    // Checks if the id exists in the array of nodes
    public boolean id_exists(int id, Node[] nodes) {
        boolean exists = false;

        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i].id == id) {
                exists = true;
            }
        }

        return exists;
    }

    
    public String convertToCSV(String[] data) {
        return Stream.of(data)
          .map(this::escapeSpecialCharacters)
          .collect(Collectors.joining(";"));
    }

    public String escapeSpecialCharacters(String data) {
        String escapedData = data.replaceAll("\\R", " ");
        if (data.contains(",") || data.contains("\"") || data.contains("'")) {
            data = data.replace("\"", "\"\"");
            escapedData = "\"" + data + "\"";
        }
        return escapedData;
    }

    public void givenDataArray_whenConvertToCSV_thenOutputCreated(List<String[]> stats) throws IOException {
        File csvOutputFile = new File("results.csv");
        try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
            stats.stream()
              .map(this::convertToCSV)
              .forEach(pw::println);
        }
    }

}
