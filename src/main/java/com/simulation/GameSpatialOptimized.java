package com.simulation;

import org.graphstream.algorithm.generator.*;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.view.Viewer;

import java.util.Random;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class GameSpatialOptimized {

    // Game Attributes
    public double p = 1; // proportion of IDN
    public double phi = 0.2; // proportion of Malicious nodes
    public double c_a = 1; // cost of action attack
    public double c_m = 1; // cost of action monitor
    public double g_a = 5; // gain on successfull attack, also loss on failed defense
    public double alpha = 0.9; // detection rate
    public double beta = 0.005; // false alarm rate
    public double phi_malicious = 0.9;

    // Simulation setup
    public int nodes_number = 200; // total number of nodes
    public int plays_per_turn = 50; // number of actions per turn
    public int max_turns = 200; // number of turns played

    // Nodes
    public Node[] nodes; // the nodes in the network

    // History of the game
    public Node[][] game_history;

    // Variables for representing the graph
    public Graph graph; // The graph representing the network and connections
    public Generator gen; // The generator that generates connections
    public Viewer viewer;
    public Tools tools = new Tools();

    public double p_x = 0.2; // IDN exchange probability

    // Constructor
    public GameSpatialOptimized() {
        game_history = new Node[max_turns][nodes_number];
        generate_graph();

        nodes = initialize_game();
        shuffle_nodes();

        apply_style_to_graph();

        initialize_beliefs();
    }

    // Initializes nodes, sets the number of malicious, normal and IDN nodes
    public Node[] initialize_game() {
        Node[] nodes = new Node[(int) nodes_number];

        int number_malicious = (int) (nodes_number * phi);

        for (int i = 0; i < number_malicious; i++) {
            Node malicious = new Node(i, p, phi, c_a, c_m, g_a, alpha, beta, "Malicious");
            nodes[i] = malicious;
        }


        int number_normal = (int) nodes_number - number_malicious;
        int number_IDN = (int) (number_normal * p);

        for (int i = number_malicious; i < number_malicious + number_IDN; i++) {
            Node idn = new Node(i, p, phi, c_a, c_m, g_a, alpha, beta, "IDN");
            nodes[i] = idn;
        }

        for (int i = number_malicious + number_IDN; i < nodes_number; i++) {
            Node normal = new Node(i, p, phi, c_a, c_m, g_a, alpha, beta, "Normal");
            nodes[i] = normal;
        }

        return nodes;
    }


    public void apply_style_to_graph() {
        graph.setAttribute("ui.stylesheet", "url('C:\\Users\\MSI\\Desktop\\PHD-Track\\Expérimentation\\Simulations\\simulation\\src\\main\\java\\com\\simulation\\stylesheet.css')");

        for (int i = 0; i < nodes_number; i++) {
            if (nodes[i].type.equals("Malicious")) {
                graph.getNode(i).setAttribute("ui.class", "malicious");
            } else if (nodes[i].type.equals("IDN")) {
                graph.getNode(i).setAttribute("ui.class", "idn");
            } else {
                graph.getNode(i).setAttribute("ui.class", "normal");
            }
        }
    }

    public Node[] get_normal_nodes() {
        Node[] normal = new Node[0];

        for (int i = 0; i < nodes_number; i++) {
            if (nodes[i].type.equals("Normal")) {
                normal = tools.add_node(normal, nodes[i]);
            }
        }

        return normal;
    }

    public void exchange_roles() {
        Random random = new Random();
        if (p != 1) {
            for (int i = 0; i < nodes_number; i++) {
                if (nodes[i].type.equals("IDN") & !nodes[i].detected) {
                    boolean picked = false;
                    double random_double = random.nextDouble();
                    if (random_double < p_x) {
                        while (!picked) {
                            Node[] normal_nodes = get_normal_nodes();
                            nodes[i].type = "Normal";
                            graph.getNode(i).setAttribute("ui.class", "normal");
                            int random_int = random.nextInt(normal_nodes.length);
                            if (!nodes[normal_nodes[random_int].id].detected) {
                                nodes[normal_nodes[random_int].id].type = "IDN";
                                graph.getNode(normal_nodes[random_int].id).setAttribute("ui.class", "idn");
                                picked = true;
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean has_IDN_neighbour(int id) {
        boolean has_IDN_neighbour = false;

        Stream<org.graphstream.graph.Node> stream_neighbors = graph.getNode(nodes[id].id).neighborNodes();
        org.graphstream.graph.Node[] array_neighbours = stream_neighbors.toArray(org.graphstream.graph.Node[]::new);
        Node[] neighbours = new Node[array_neighbours.length];
        for (int j = 0; j < array_neighbours.length; j++) {
            neighbours[j] = nodes[array_neighbours[j].getIndex()];
        }

        for (int i = 0; i < neighbours.length; i++) {
            if (neighbours[i].type.equals("IDN")) {
                has_IDN_neighbour = true;
            }
        }

        return has_IDN_neighbour;
    }

    public void generate_graph() {
        graph = new SingleGraph("Spatial Simulation");

        gen = new DorogovtsevMendesGenerator();

        gen.addSink(graph);
        gen.begin();

        // always generates 3 nodes at graph creation !!!!! DEPENDS ON THE TYPE OF GRAPH
        for (int i = 0; i < nodes_number - 3; i++) {
            gen.nextEvents();
        }

        gen.end();
        //viewer = graph.display(true);
    }

    // Randomly shuffles the position of nodes inside the network
    public void shuffle_nodes() {
        Random rand = new Random();

        for (int i = 0; i < nodes.length; i++) {
            int randomIndexToSwap = rand.nextInt(nodes.length);
            Node temp = nodes[randomIndexToSwap];
            nodes[randomIndexToSwap] = nodes[i];
            nodes[i] = temp;
        }

        for (int i = 0; i < nodes.length; i++) {
            nodes[i].id = i;
        }
    }

    public void play_game() throws InterruptedException {
        for (int i = 0; i < max_turns; i++) {
            play_turn();

            game_history[i] = new Node[nodes_number];

            for (int j = 0; j < nodes_number; j++) {
                game_history[i][j] = new Node(nodes[j]);
            }
            exchange_roles();
        }
    }

    public void play_turn() {
        for (int i = 0; i < plays_per_turn; i++) {
            Node sender = pick_sender_node();
            Node receiver = pick_receiver_node(sender);

            if (sender != null && receiver != null) {
                take_actions(sender, receiver);
            }
        }
    }

    public void take_actions(Node sender, Node receiver) {
        sender.play("Sender");
        receiver.play("Receiver");

        Random rand = new Random();

        if (sender.type.equals("Malicious")) {
            // True detection of the malicious node
            if (sender.current_action.equals("Attack")) {
                if (receiver.current_action.equals("Monitor")) {
                    double double_random = rand.nextDouble();
                    if (double_random <= alpha) {
                        sender.detected = true;
                        graph.getNode(sender.id).setAttribute("ui.class", "detected");
                    }
                }
            }
            // False detection of the malicious node
            if (sender.current_action.equals("Not")) {
                if (receiver.current_action.equals("Monitor")) {
                    double double_random = rand.nextDouble();
                    if (double_random <= beta) {
                        sender.detected = true;
                    }
                }
            }
        } else { // The case of false detecting a non-malicious node
            if (receiver.current_action.equals("Monitor")) {
                double double_random = rand.nextDouble();
                if (double_random <= beta) {
                    sender.detected = true;
                }
            }
        }
    }

    public Node pick_sender_node() {
        Node sender = null;
        boolean picked = false;
        Random rand = new Random();

        while (!picked) {
            int int_random = rand.nextInt(nodes_number);
            Node selected_node = nodes[int_random];
            if (!selected_node.detected & !selected_node.ran_out_of_power & sender_has_valid_neighbors(selected_node.id)) {
                sender = selected_node;
                picked = true;
            }
        }

        return sender;
    }

    public Node pick_receiver_node(Node sender) {
        Node receiver = null;
        boolean picked = false;
        Random rand = new Random();

        Stream<org.graphstream.graph.Node> stream_neighbors = graph.getNode(sender.id).neighborNodes();
        org.graphstream.graph.Node[] array_neighbours = stream_neighbors.toArray(org.graphstream.graph.Node[]::new);
        Node[] neighbours = new Node[array_neighbours.length];
        for (int i = 0; i < array_neighbours.length; i++) {
            neighbours[i] = nodes[array_neighbours[i].getIndex()];
        }

        while (!picked) {
            int int_random = rand.nextInt(neighbours.length);
            Node selected_node = neighbours[int_random];
            if (!selected_node.type.equals("Malicious") & !selected_node.detected & !selected_node.ran_out_of_power) {
                receiver = selected_node;
                picked = true;
            }
        }

        return receiver;
    }

    public boolean sender_has_valid_neighbors(int id) {
        boolean has_valid_neighbors = false;

        Stream<org.graphstream.graph.Node> stream_neighbors = graph.getNode(id).neighborNodes();
        org.graphstream.graph.Node[] array_neighbours = stream_neighbors.toArray(org.graphstream.graph.Node[]::new);
        Node[] neighbours = new Node[array_neighbours.length];

        for (int i = 0; i < array_neighbours.length; i++) {
            neighbours[i] = nodes[array_neighbours[i].getIndex()];
        }

        for (int i = 0; i < neighbours.length; i++) {
            if (!neighbours[i].type.equals("Malicious") & !neighbours[i].detected & !neighbours[i].ran_out_of_power) {
                has_valid_neighbors = true;
            }
        }

        return has_valid_neighbors;
    }

    public void play_game_dynamic() throws InterruptedException {
        for (int i = 0; i < max_turns; i++) {
            play_turn_dynamic();

            game_history[i] = new Node[nodes_number];

            for (int j = 0; j < nodes_number; j++) {
                game_history[i][j] = new Node(nodes[j]);
            }
            exchange_roles();
        }
    }

    public void play_turn_dynamic() {
        for (int i = 0; i < plays_per_turn; i++) {
            Node sender = pick_sender_node();
            Node receiver = pick_receiver_node(sender);

            if (sender != null && receiver != null) {
                take_actions_dynamic(sender, receiver);
            }
        }
    }

    public void initialize_beliefs() {
        for (int i = 0; i < nodes.length; i++) {
            nodes[i].beliefs_p = new double[nodes_number];
            nodes[i].beliefs_phi = new double[nodes_number];
            for (int j = 0; j < nodes.length; j++) {
                nodes[i].beliefs_p[j] = p;
                nodes[i].beliefs_phi[j] = phi;
            }
        }
    }

    public void take_actions_dynamic(Node sender, Node receiver) {

        double belief_p = sender.beliefs_p[receiver.id];
        double belief_phi = receiver.beliefs_phi[sender.id];

        sender.play_dynamic("Sender", belief_phi, belief_p);
        receiver.play_dynamic("Receiver", belief_phi, belief_p);


        Random rand = new Random();

        double monitor_prob = 0;
        double attack_prob = 0;

        if (belief_p == 1) {
            if (belief_phi < sender.phi_threshold) {
                attack_prob = 1;
                monitor_prob = 0;
            } else {
                attack_prob = (c_m + beta * g_a) / (belief_phi * g_a * (2 * alpha + beta));
                monitor_prob = (g_a - c_a) / (2 * belief_p * alpha * g_a);
            }
        } else {
            if (belief_p < receiver.p_threshold & belief_phi > sender.phi_threshold) {
                attack_prob = 1;
                monitor_prob = 1;
            } else if (belief_p < receiver.p_threshold & belief_phi < sender.phi_threshold) {
                attack_prob = 1;
                monitor_prob = 0;
            } else if (belief_p > receiver.p_threshold & belief_phi < sender.phi_threshold) {
                attack_prob = 1;
                monitor_prob = 0;
            } else if (belief_p > receiver.p_threshold & belief_phi > sender.phi_threshold) {
                attack_prob = (c_m + beta * g_a) / (belief_phi * g_a * (2 * alpha + beta));
                monitor_prob = (g_a - c_a) / (2 * belief_p * alpha * g_a);
            }
        }

        if (p == 1) {
            if (sender.type.equals("Malicious")) {
                if (sender.current_action.equals("Attack")) {
                    if (receiver.current_action.equals("Monitor")) {
                        double double_random = rand.nextDouble();
                        if (double_random <= alpha) { //if it observed an attack
                            receiver.beliefs_phi[sender.id] = belief_phi * (alpha * attack_prob + beta * (1 - attack_prob)) / (belief_phi * (alpha * attack_prob + beta * (1 - attack_prob)) + (1 - belief_phi) * beta);
                        } else { //didn't observe an attack
                            receiver.beliefs_phi[sender.id] = belief_phi * ((1 - alpha) * attack_prob + (1 - beta) * (1 - attack_prob)) / (belief_phi * ((1 - alpha) * attack_prob + (1 - beta) * (1 - attack_prob)) + (1 - belief_phi) * (1 - beta));
                        }
                    }
                }
                if (sender.current_action.equals("Not")) {
                    if (receiver.current_action.equals("Monitor")) {
                        double double_random = rand.nextDouble();
                        if (double_random <= beta) { // if it observed an attack
                            receiver.beliefs_phi[sender.id] = belief_phi * (alpha * attack_prob + beta * (1 - attack_prob)) / (belief_phi * (alpha * attack_prob + beta * (1 - attack_prob)) + (1 - belief_phi) * beta);

                        } else { //if it didn't observe an attack
                            receiver.beliefs_phi[sender.id] = belief_phi * ((1 - alpha) * (1 - attack_prob) + (1 - beta) * (1 - alpha)) / (belief_phi * ((1 - alpha) * (1 - attack_prob) + (1 - beta) * (1 - alpha)) + (1 - belief_phi) * (1 - beta));

                        }
                    }
                }
            } else { // The case of false detecting a non-malicious node
                if (receiver.current_action.equals("Monitor")) {
                    double double_random = rand.nextDouble();
                    if (double_random <= beta) { // if it observed an attack
                        receiver.beliefs_phi[sender.id] = belief_phi * (alpha * attack_prob + beta * (1 - attack_prob)) / (belief_phi * (alpha * attack_prob + beta * (1 - attack_prob)) + (1 - belief_phi) * beta);
                    } else { //if it didn't observe an attack
                        receiver.beliefs_phi[sender.id] = belief_phi * ((1 - alpha) * (1 - attack_prob) + (1 - beta) * (1 - alpha)) / (belief_phi * ((1 - alpha) * (1 - attack_prob) + (1 - beta) * (1 - alpha)) + (1 - belief_phi) * (1 - beta));
                    }
                }
            }
        } else {
            if (sender.type.equals("Malicious")) {
                if (sender.current_action.equals("Attack")) {
                    if (receiver.current_action.equals("Monitor")) {
                        double double_random = rand.nextDouble();
                        if (double_random <= alpha) { //if it observed an attack
                            receiver.beliefs_phi[sender.id] = belief_phi * (alpha * attack_prob + beta * (1 - attack_prob)) / (belief_phi * (alpha * attack_prob + beta * (1 - attack_prob)) + (1 - belief_phi) * beta);
                            sender.beliefs_p[receiver.id] = belief_p * (monitor_prob * alpha) / (belief_p * (monitor_prob * alpha) + (1 - belief_p) * 0);
                        } else { //didn't observe an attack
                            receiver.beliefs_phi[sender.id] = belief_phi * ((1 - alpha) * attack_prob + (1 - beta) * (1 - attack_prob)) / (belief_phi * ((1 - alpha) * attack_prob + (1 - beta) * (1 - attack_prob)) + (1 - belief_phi) * (1 - beta));
                            sender.beliefs_p[receiver.id] = belief_p * (1 - monitor_prob * alpha) / (belief_p * (1 - monitor_prob * alpha) + (1 - belief_p) * 1);
                        }
                    }
                }
                if (sender.current_action.equals("Not")) {
                    if (receiver.current_action.equals("Monitor")) {
                        double double_random = rand.nextDouble();
                        if (double_random <= beta) { // if it observed an attack
                            receiver.beliefs_phi[sender.id] = belief_phi * (alpha * attack_prob + beta * (1 - attack_prob)) / (belief_phi * (alpha * attack_prob + beta * (1 - attack_prob)) + (1 - belief_phi) * beta);
                            sender.beliefs_p[receiver.id] = 1;
                        } else { //if it didn't observe an attack
                            receiver.beliefs_phi[sender.id] = belief_phi * ((1 - alpha) * (1 - attack_prob) + (1 - beta) * (1 - alpha)) / (belief_phi * ((1 - alpha) * (1 - attack_prob) + (1 - beta) * (1 - alpha)) + (1 - belief_phi) * (1 - beta));
                            sender.beliefs_p[receiver.id] = 1;
                        }
                    }
                }
            } else { // The case of false detecting a non-malicious node
                if (receiver.current_action.equals("Monitor")) {
                    double double_random = rand.nextDouble();
                    if (double_random <= beta) { // if it observed an attack
                        receiver.beliefs_phi[sender.id] = belief_phi * (alpha * attack_prob + beta * (1 - attack_prob)) / (belief_phi * (alpha * attack_prob + beta * (1 - attack_prob)) + (1 - belief_phi) * beta);
                        sender.beliefs_p[receiver.id] = 1;
                    } else { //if it didn't observe an attack
                        receiver.beliefs_phi[sender.id] = belief_phi * ((1 - alpha) * (1 - attack_prob) + (1 - beta) * (1 - alpha)) / (belief_phi * ((1 - alpha) * (1 - attack_prob) + (1 - beta) * (1 - alpha)) + (1 - belief_phi) * (1 - beta));
                        sender.beliefs_p[receiver.id] = 1;
                    }
                }
            }

            if (receiver.beliefs_phi[sender.id] > phi_malicious) {
                sender.detected = true;
            }
        }

        if (receiver.beliefs_phi[sender.id] > phi_malicious) {
            sender.detected = true;
            if (sender.type.equals("Malicious")) {
                graph.getNode(sender.id).setAttribute("ui.class", "detected");
            }
        }
    }

    public List<String[]> get_Stats() {
        List<String[]> stats = new ArrayList<>();

        for (int i = 0; i < max_turns; i++) {
            double number_detected = 0;
            double number_false_detected = 0;
            double total_monitoring_costs = 0;

            for (int j = 0; j < nodes_number; j++) {
                if (game_history[i][j].detected & game_history[i][j].type.equals("Malicious")) {
                    number_detected++;
                } else if (game_history[i][j].detected & !game_history[i][j].type.equals("Malicious")) {
                    number_false_detected++;
                } else if (!game_history[i][j].type.equals("Malicious")) {
                    total_monitoring_costs += game_history[i][j].max_energy_level - game_history[i][j].energy_level;
                }
            }

            stats.add(new String[]{String.valueOf(i + 1), String.valueOf((int) number_detected),
                    String.valueOf((int) number_false_detected), String.valueOf((int) total_monitoring_costs)});
        }

        return stats;
    }
}
