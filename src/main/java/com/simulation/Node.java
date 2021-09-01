package com.simulation;

import java.util.Random;

public class Node {

    // Node attributes
    public int id; // id of the node
    public double p; // proportion of IDN
    public double phi; // proportion of Malicious nodes
    public double c_a; // cost of action attack
    public double c_m; // cost of action monitor
    public double g_a; // gain on successfull attack, also loss on failed defense
    public double alpha; // detection rate
    public double beta; // false alarm rate
    public double energy_level = 100000; // the energy level of the node
    public double max_energy_level = 100000; //the maximum energy level of the node
    public String type;
    public double p_threshold;
    public double phi_threshold;
    public String current_action;
    public double attack_probability = 0;
    public double monitor_probability = 0;
    public boolean detected = false;
    public boolean ran_out_of_power = false;
    public double [] beliefs_p;
    public double [] beliefs_phi;

    // Constructor from attributes
    public Node(int id, double p, double phi, double c_a, double  c_m, double g_a, double alpha, double beta, String type){
        this.id = id;
        this.p = p;
        this.phi = phi;
        this.c_a = c_a;
        this.c_m = c_m;
        this.g_a = g_a;
        this.type = type;
        this.alpha = alpha;
        this.beta = beta;
        this.p_threshold = (g_a - c_a)/(2*alpha*g_a);
        this.phi_threshold = (beta*g_a + c_m)/(g_a*(2*alpha+beta));
    }

    // Constructor by copy
    public Node (Node node){
        this.id = node.id;
        this.p = node.p;
        this.phi = node.phi;
        this.c_a = node.c_a;
        this.c_m = node.c_m;
        this.g_a = node.g_a;
        this.type = node.type;
        this.alpha = node.alpha;
        this.beta = node.beta;
        this.p_threshold = node.p_threshold;
        this.phi_threshold = node.phi_threshold;
        this.energy_level = node.energy_level;
        this.max_energy_level = node.max_energy_level;
        this.detected = node.detected;
    }

    public void play(String role){
        if(p == 1){
            if(phi < phi_threshold){ // The pure-strategy case where IDN doesn't monitor and Malicious Node always attacks
                if(type.equals("Malicious")){
                    this.current_action = "Attack";
                    energy_level--;
                }else if(type.equals("IDN")){
                    this.current_action = "Not";
                }else{
                    this.current_action = "Not";
                }
            }else{ // The mixed-strategy where Malicious and IDN take actions at certain probabilities
                if(type.equals("Malicious")){
                    attack_probability = (c_m + beta * g_a)/(phi*g_a*(2*alpha+beta));
                    Random rand = new Random();
                    double double_random= rand.nextDouble();
                    if(double_random <= attack_probability){
                        this.current_action = "Attack"; 
                        energy_level--;
                    }else{
                        this.current_action = "Not"; 
                    }
                }else if(type.equals("IDN")){
                    monitor_probability = (g_a - c_a)/(2*alpha*g_a);
                    Random rand = new Random();
                    double double_random= rand.nextDouble();
                    if(double_random <= monitor_probability){
                        this.current_action = "Monitor"; 
                        if(role.equals("Receiver")){ // doesn't lose energy if it's a Sender
                            energy_level--;
                        }
                    }else{
                        this.current_action = "Not"; 
                    }
                }else{
                    this.current_action = "Not";
                }
            }
        }else{ // Our model where p != 1
            if(type.equals("Malicious")){
                if(p < p_threshold & phi > phi_threshold){
                    this.current_action = "Attack";
                    energy_level--;
                }else if(p < p_threshold & phi < phi_threshold){
                    this.current_action = "Attack"; 
                    energy_level--;
                }else if(p > p_threshold & phi < phi_threshold){
                    this.current_action = "Attack"; 
                    energy_level--;
                }else if(p > p_threshold & phi > phi_threshold){
                    attack_probability = (c_m + beta * g_a)/(phi*g_a*(2*alpha+beta));
                    Random rand = new Random();
                    double double_random= rand.nextDouble();
                    if(double_random <= attack_probability){
                        this.current_action = "Attack"; 
                        energy_level--;
                    }else{
                        this.current_action = "Not"; 
                    }
                }
            }else if(type.equals("IDN")){
                if(p <= p_threshold & phi >= phi_threshold){
                    this.current_action = "Monitor";
                    if(role.equals("Receiver")){
                        energy_level--;
                    }
                }else if(p < p_threshold & phi < phi_threshold){
                    this.current_action = "Not"; 
                }else if(p > p_threshold & phi < phi_threshold){
                    this.current_action = "Not"; 
                }else if(p > p_threshold & phi > phi_threshold){
                    monitor_probability = (g_a - c_a)/(2*alpha*p*g_a);
                    Random rand = new Random();
                    double double_random= rand.nextDouble();
                    if(double_random <= monitor_probability){
                        this.current_action = "Monitor"; 
                        if(role.equals("Receiver")){
                            energy_level--;
                        }
                    }else{
                        this.current_action = "Not"; 
                    }
                }
            }else if(type.equals("Normal")){
                this.current_action = "Not";
            }
        }

        if(energy_level == 0){
            ran_out_of_power = true;
        }
    }

    public void play_dynamic(String role, double belief_phi, double belief_p){

        if(type.equals("Normal")){
            monitor_probability = 0;
        }

        if(belief_p == 1){
            if(belief_phi < phi_threshold){ // The pure-strategy case where IDN doesn't monitor and Malicious Node always attacks
                if(type.equals("Malicious")){
                    attack_probability = 1;
                    this.current_action = "Attack";
                    //energy_level--;
                }else if(type.equals("IDN")){
                    this.current_action = "Not";
                }else{
                    this.current_action = "Not";
                }
            }else{ // The mixed-strategy where Malicious and IDN take actions at certain probabilities
                if(type.equals("Malicious")){
                    attack_probability = (c_m + beta * g_a)/(belief_phi*g_a*(2*alpha+beta));
                    Random rand = new Random();
                    double double_random= rand.nextDouble();
                    if(double_random <= attack_probability){
                        this.current_action = "Attack";
                        //energy_level--;
                    }else{
                        this.current_action = "Not";
                    }
                }else if(type.equals("IDN")){
                    monitor_probability = (g_a - c_a)/(2*alpha*g_a);
                    Random rand = new Random();
                    double double_random= rand.nextDouble();
                    if(double_random <= monitor_probability){
                        this.current_action = "Monitor";
                        if(role.equals("Receiver")){ // doesn't lose energy if it's a Sender
                            energy_level--;
                        }
                    }else{
                        this.current_action = "Not";
                    }
                }else{
                    this.current_action = "Not";
                }
            }
        }else{ // Our model where p != 1
            if(type.equals("Malicious")){
                if(belief_p < p_threshold & belief_phi > phi_threshold){
                    attack_probability = 1;
                    this.current_action = "Attack";
                    //energy_level--;
                }else if(belief_p < p_threshold & belief_phi < phi_threshold){
                    attack_probability = 1;
                    this.current_action = "Attack";
                    //energy_level--;
                }else if(belief_p > p_threshold & belief_phi < phi_threshold){
                    attack_probability = 1;
                    this.current_action = "Attack";
                    //energy_level--;
                }else if(belief_p > p_threshold & belief_phi > phi_threshold){
                    attack_probability = (c_m + beta * g_a)/(belief_phi*g_a*(2*alpha+beta));
                    Random rand = new Random();
                    double double_random= rand.nextDouble();
                    if(double_random <= attack_probability){
                        this.current_action = "Attack";
                        //energy_level--;
                    }else{
                        this.current_action = "Not";
                    }
                }
            }else if(type.equals("IDN")){
                if(belief_p <= p_threshold & belief_phi >= phi_threshold){
                    this.current_action = "Monitor";
                    if(role.equals("Receiver")){
                        energy_level--;
                    }
                }else if(belief_p < p_threshold & belief_phi < phi_threshold){
                    this.current_action = "Not";
                }else if(belief_p > p_threshold & belief_phi < phi_threshold){
                    this.current_action = "Not";
                }else if(belief_p > p_threshold & belief_phi > phi_threshold){
                    monitor_probability = (g_a - c_a)/(2*alpha*belief_p*g_a);
                    Random rand = new Random();
                    double double_random= rand.nextDouble();
                    if(double_random <= monitor_probability){
                        this.current_action = "Monitor";
                        if(role.equals("Receiver")){
                            energy_level--;
                        }
                    }else{
                        this.current_action = "Not";
                    }
                }
            }else if(type.equals("Normal")){
                this.current_action = "Not";
            }
        }

        if(energy_level == 0){
            ran_out_of_power = true;
        }
    }

}
