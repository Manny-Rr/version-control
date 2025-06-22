package src;

import gitlet.Utils;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;



public class Commit implements Serializable{

    //Commit variables to store information
    private String message, timestamp, hash, branch;
    //private Commit parent;
    private HashMap<Commit, Integer> parents = new HashMap<>();
    //Hashmap  - Key: File Name, Value: Location of the file
    private HashMap<String, File> files = new HashMap<>();

    //Constructor for only init command
    public Commit(String message, String timestamp, Commit parent){
        this.message = message;
        this.timestamp = timestamp;
    }

    //returns message of the commit
    public String get_msg(){return this.message;}
    public HashMap<Commit, Integer> get_parents_map(){
        return this.parents;
    }
    public Commit get_active_parent(){
        for (Commit c : parents.keySet()){
            if (parents.get(c) == 1) return c;
        }
        return null;
    }

    public String get_sha(){
        return Utils.sha1(Utils.serialize(this));
    }

    public String get_timestamp(){return this.timestamp;}
    public HashMap<String, File> get_files(){return this.files;}
    //for setting and getting branch
    public void set_branch(String branchname){
        this.branch = branchname;

    }
    public String get_branch(){return this.branch;}

    //for setting and getting hash
    public void set_hash(String hash){this.hash=hash;}
    public String get_hash(){return this.hash;}

    public void set_commit(String message, String timestamp, Commit parent){
        this.message = message;
        this.timestamp = timestamp;
        this.parents.clear();
        this.parents.put(parent, 1);
    }

    //Keep this method for debugging
    public void print_commit() {
        System.out.println(this.message);
        System.out.println(this.timestamp);
        System.out.println(this.hash);
        System.out.println(this.branch);
        for (String f : files.keySet()) {
            System.out.print(f+ " ");
            System.out.println(files.get(f));
        }
        for (Commit c : parents.keySet()) {
            System.out.print(c+ " ");
            System.out.println(parents.get(c));
        }
    }

    @Override
    public boolean equals(Object c){
        if (this == c) return true;
        Commit a = (Commit) c;
        if (this.get_hash().compareTo(a.get_hash()) == 0){
            return true;
        }
        return false;
    }

    public boolean compare_file(String s, Commit c){
        return this.files.get(s).getAbsolutePath().compareTo(c.get_files().get(s).getAbsolutePath()) == 0;
    }
}
