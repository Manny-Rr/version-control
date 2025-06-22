package src;

import gitlet.Utils;

import java.io.*;
import static java.lang.System.out;
import static gitlet.Utils.*;;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;


//This class calls on basic repository functions such init

public class Repository implements Serializable {


    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    //Directory for the staging area
    public static final File staging = join(GITLET_DIR, "staging");
    public static final File addition = join(staging, "addition");
    public static final File removal = join(staging, "removal");
    public static final File addition_set = join(staging, "addition_set");
    public static final File removal_set = join(staging, "removal_set");
    public static final File branch_map = join(GITLET_DIR, "branch_map");
    public static final File merge = join(GITLET_DIR, "merge commit");
    //Directory for the Commiting area
    public static final File commits = join(GITLET_DIR, "commits");
    //Directory for the Blobs area
    public static final File blobs = join(GITLET_DIR, "Blobs (Data)");
    public static final File master_file = join(GITLET_DIR, "Master");
    public static final File head_file = join(GITLET_DIR, "Head");
    public static Commit Master;
    public static Commit Head;
    //public static boolean next_commit_merge;


    //Initializes the repository with a .gitlet folder and creates an initial commit.
    public static void init(){
        if (GITLET_DIR.exists())
        {   
            out.println("A Gitlet version-control system already exists in the current directory."); return;
        }
        GITLET_DIR.mkdir(); staging.mkdir(); commits.mkdir(); blobs.mkdir();
        addition.mkdir(); removal.mkdir();

        //initialize the additions hashset
        HashSet<String> addition_hash = new HashSet<>();
        HashSet<String> removal_hash = new HashSet<>();
        writeObject(addition_set, addition_hash);
        writeObject(removal_set, removal_hash);

        //Create initial commit, for date each time call the to.String method
        Commit initial_commit = new Commit("Initial Commit", " 00:00:00 UTC, Thursday, 1 January 1970", null);
        String initial_hash = sha1((serialize(initial_commit)));
        initial_commit.set_hash(initial_hash);
        initial_commit.set_branch("Master");
        //All branches, integer 1 to show its the currently active branch
        HashMap<String, Integer> branches = new HashMap<>();
        branches.put(initial_commit.get_branch(), 1);
        writeObject(branch_map, branches);

        File to_commit = Utils.join(commits, initial_hash);
        writeObject(to_commit, initial_commit);

        //if the next commit is the result of a merge
        writeObject(merge, false);

        //for master and head
        Master = Head = initial_commit;
        writeObject(master_file, initial_commit);
        writeObject(head_file, initial_commit);
    }

    public static void add(String file){
        //Check if the file to be added even exists in the current working directory
        //if it does not, then print error and return
        File to_add = join(CWD, file);
        if (!to_add.exists()){out.println("File does not exist."); return;}
        //hash of the file to be added
        //1. Read the content and get its hash
        String hash = sha1(readContents(to_add));
        //out.println(hash);   //for testing purposes

        //open up the hashset
        @SuppressWarnings("unchecked")
        HashSet<String> set = readObject(addition_set, HashSet.class);


        //replace the file that shares the same name, write file and return immediately
        if (set.contains(file)){
            File[] files_to_add = addition.listFiles();
            for (File f : files_to_add){
                String name = f.getName().substring(40);
                if (name.compareTo(file) == 0) {
                    f.delete();
                    writeObject(join(addition, hash + file), readContents(to_add));
                    return;
                }
            }
        }

        //2.Load the current head commit
        Commit current_commit = readObject(head_file, Commit.class);
        //3. HashMap - Check if the commit contains a key with the given file name and check its contents/hash
        //check if the commit contains a key with this name
        if (current_commit.get_files().containsKey(file)){
            //if it does, get it's value through .get
            File f = current_commit.get_files().get(file);
            //get the hash of the value (file location)
            String commit_hash  = sha1(readObject(f, byte[].class));
            //compare the 2 hashes, if they are the same do nothing
            if (hash.compareTo(commit_hash) == 0){return;}
        }
        //if it doesn't contain the key or the files aren't the same, add it to staging with it's hash as name
        writeObject(join(addition, hash + file), readContents(to_add));
        //write the file by name to the hashset for accessing later
        set.add(file);
        writeObject(addition_set, set);
        //writeContents(join(addition, hash + file), readContents(to_add));
    }
    //remove command
    public static void rm(String file){

        Commit current_commit = readObject(head_file, Commit.class);
        @SuppressWarnings("unchecked")
        HashSet<String> set = readObject(addition_set, HashSet.class);

        if (!current_commit.get_files().containsKey(file) && !set.contains(file)){
            out.println("No reason to remove the file."); return;
        }

        //Check if the file to be removed is staged for commit, if it is then unstage it
        if (set.contains(file)){
            File[] files_to_unstage = addition.listFiles();
            for (File f : files_to_unstage){
                String name = f.getName().substring(40);
                if (name.compareTo(file) == 0) {
                    f.delete();
                    set.remove(file);
                    writeObject(addition_set, set);
                    return;
                }
            }
        }

        @SuppressWarnings("unchecked")
        HashSet<String> r_set = readObject(removal_set, HashSet.class);
        //3. HashMap - Check if the commit contains a key with the given file name and check its contents/hash
        //check if the commit contains a key with this name
        if (current_commit.get_files().containsKey(file)){
            File to_remove = Utils.join(CWD, file);
            to_remove.renameTo(join(removal, file));
            //if the current commit does contain the file as key, then stage it for removal
            r_set.add(file);
            writeObject(removal_set, r_set);
        }

    }

    public static void commit(String message){
        //First, check there are files in the staging directory
        //going to use additions and removal hashset
        @SuppressWarnings("unchecked")
        HashSet<String> set = readObject(addition_set, HashSet.class);
        @SuppressWarnings("unchecked")
        HashSet<String> r_set = readObject(removal_set, HashSet.class);

        //check if additions and removal are empty
        if (set.isEmpty() && r_set.isEmpty()) {out.println("No changes added to the commit."); return;}

        //Clone the parent commit
        Commit cloned_commit = readObject(head_file, Commit.class);
        //Change the required values of the clone
        cloned_commit.set_commit(message, new Date().toString(), readObject(head_file, Commit.class));


        if(!set.isEmpty()){
            File[] files_to_add = addition.listFiles();
            for (File f : files_to_add) {
                //their new location in the blobs directory under the first 2 characters of it's hash
                String sub = f.getName().substring(0, 2);
                String rest = f.getName().substring(2, 40);
                String actual_name = f.getName().substring(40);

                File new_directory = join(blobs, sub);
                if (!new_directory.exists()){new_directory.mkdir();}
                //Location of the new file in blobs
                File new_location = join(new_directory, rest);
                //writeObject(new_location, a);
                f.renameTo(new_location);
                //Change the File values in the hashmap with the associated key
                cloned_commit.get_files().put(actual_name, new_location);
                //Delete the file in the staging directory
                f.delete();
            }
            set.clear();
            writeObject(addition_set, set);
        }

        //for all keys within removal hashset, remove the key from the commit meaning it will no longer track that blob.
        if(!r_set.isEmpty()){
            for (String s : r_set){
                cloned_commit.get_files().remove(s);
                File f = join(removal, s);
                f.delete();
            }
            r_set.clear();
            writeObject(removal_set, r_set);
        }
        //get the sha-1 hash of the commit as save it using that
        String clone_commit_hash = sha1(serialize(cloned_commit));
        cloned_commit.set_hash(clone_commit_hash);


        boolean next_commit_merge = readObject(merge, Boolean.class);
        if (next_commit_merge){
            Commit new_parent = readObject(join(GITLET_DIR, get_active_branch()), Commit.class);
            cloned_commit.get_parents_map().put(new_parent, 0);
            set_active_branch(cloned_commit.get_branch());
            writeObject(merge, false);
        }

        //depending on the branchname of the cloned commit, it will decide how to move the HEAD pointer
        String branchname = get_active_branch();
        cloned_commit.set_branch(branchname);

        //write the commit object and move the HEAD pointer
        writeObject(join(commits, clone_commit_hash), cloned_commit);

        //for writing the branch like head
        writeObject(join(GITLET_DIR, branchname), cloned_commit);

        writeObject(head_file, cloned_commit);


    }

    //Recursive logging
    public static void log(){
        //for logging all the commit starting at the head commit all the way to the inital commit
        Commit head = readObject(head_file, Commit.class);
        log(head);
    }
    private static void log(Commit c){
        if (c == null){return;}

        out.println("===");
        out.println("commit " + c.get_hash());
        if (c.get_parents_map().size() > 1){
            out.print("Merge: ");
            for (Commit a: c.get_parents_map().keySet()){
                out.print(a.get_hash().substring(0 , 7) + " ");
            }
            out.println();
        }
        out.println("Date: " + c.get_timestamp());
        out.println(c.get_msg());
        //out.println(c);
        //out.println(c.get_active_parent());
        /*out.println(c.get_hash());
        out.println();*/
        log(c.get_active_parent());
    }
    public static void global_log(){
        for (File f : commits.listFiles()){
            Commit c = readObject(f, Commit.class);
            global_log(c);
        }
    }
    private static void global_log(Commit c){
        out.println("===");
        out.println("commit " + c.get_hash());
        out.println("Date: " + c.get_timestamp());
        out.println(c.get_msg());
        out.println();
    }

    //For finding a commit with the given message
    public static void find(String message){
        boolean exists = false;
        File[] commit_files = commits.listFiles();
        for (File f : commit_files){
            Commit c = readObject(f, Commit.class);
            if (message.compareTo(c.get_msg()) == 0) {
                out.println(c.get_hash());
                exists = true;
            }
        }
        if (!exists){out.println("Found no commit with that message.");}
    }
    public static void status(){
        out.println("=== Branches ===");
        @SuppressWarnings("unchecked")
        HashMap<String, Integer> branches = readObject(branch_map, HashMap.class);
        for (String s : branches.keySet()){
            if (branches.get(s) == 1)
                out.println("*" + s);
            else
                out.println(s);
        }
        out.println();

        out.println("=== Staged Files ===");
        @SuppressWarnings("unchecked")
        HashSet<String> additions = readObject(addition_set, HashSet.class);
        for (String s : additions){
            out.println(s);
        }
        out.println();

        out.println("=== Removed Files ===");
        @SuppressWarnings("unchecked")
        HashSet<String> removals = readObject(removal_set, HashSet.class);
        for (String s : removals){
            out.println(s);
        }
        out.println();

        //TODO: Print some other things but leave this for later
    }

    public static void branch(String branchname){
        Commit branch = readObject(head_file, Commit.class);
        //no need for this as at this time, branch pointer will be the same as the head and original branch
        //it will get changed on the next commit, only need to change the active branch here
        //branch.set_branch(branchname);

        @SuppressWarnings("unchecked")
        HashMap<String, Integer> branches = readObject(branch_map, HashMap.class);
        //unset all other branches
        for (String s : branches.keySet()){
            branches.replace(s, 0);
        }
        branches.put(branchname, 1);
        writeObject(branch_map, branches);
        writeObject(join(GITLET_DIR, branchname), branch);

        //save the split point commit, deleting it when merging
        Commit split_point = branch;
        writeObject(join(GITLET_DIR, branchname + " splitpoint"), split_point);
    }
    private static String get_active_branch(){
        @SuppressWarnings("unchecked")
        HashMap<String, Integer> branches = readObject(branch_map, HashMap.class);
        for (String s : branches.keySet()){
            if (branches.get(s) == 1) {return s;}
        }
        return null;
    }
    private static void set_active_branch(String branchname){
        @SuppressWarnings("unchecked")
        HashMap<String, Integer> branches = readObject(branch_map, HashMap.class);
        for (String s : branches.keySet()){
            branches.replace(s, 0);
        }
        branches.replace(branchname, 1);
        writeObject(branch_map, branches);
    }
    public static void remove_branch(String branchname){
        @SuppressWarnings("unchecked")
        HashMap<String, Integer> branches = readObject(branch_map, HashMap.class);
        //if given branch does not exist
        if (!branches.containsKey(branchname)){out.println("A branch with that name does not exist."); return;}
        if (branches.get(branchname) == 1){out.println("Cannot remove the current branch."); return;}

        File branch_to_delete = join(GITLET_DIR, branchname);
        branch_to_delete.delete();
    }

    public static void reset(String id) throws IOException {
        Commit c = commit_id(id);
        if (c == null){out.println("No commit with that id exists."); return;}
        for (String s : c.get_files().keySet()){
            byte[] bytes = readObject(c.get_files().get(s), byte[].class);
            Path path = join(CWD, s).toPath();
            Files.write(path, bytes);
        }
        remove_tracked(c);
        writeObject(join(GITLET_DIR, get_active_branch()), c);
        writeObject(head_file, c);
    }

    //Merges the branch with the given name into the current active branch
    public static void merge(String branchname) throws IOException{
        //load the required data sets/maps
        @SuppressWarnings("unchecked")
        HashMap<String, Integer> branches = readObject(branch_map, HashMap.class);
        @SuppressWarnings("unchecked")
        HashSet<String> set = readObject(addition_set, HashSet.class);
        @SuppressWarnings("unchecked")
        HashSet<String> r_set = readObject(removal_set, HashSet.class);

        //if there additions and removals staged then return statement with uncommited changes
        if (!set.isEmpty() || !r_set.isEmpty()){out.println("You have uncommitted changes."); return;}
        if (!branches.containsKey(branchname)){out.println("A branch with that name does not exist."); return;}
        if (branches.get(branchname) == 1){out.println("Cannot merge a branch with itself."); return;}

        Commit current_branch = readObject(head_file, Commit.class);
        Commit given_branch = readObject(join(GITLET_DIR, branchname), Commit.class);

        for (String s : CWD.list()){
            if (join(CWD, s).isDirectory()) continue;
            if(!current_branch.get_files().containsKey(s)){
                out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                return;
            }
        }


        Commit split_point;
        if (current_branch.get_branch().compareTo("Master") == 0) {
            split_point =readObject(join(GITLET_DIR, given_branch.get_branch() + " splitpoint"), Commit.class);
        }
        else{
            split_point =readObject(join(GITLET_DIR, current_branch.get_branch() + " splitpoint"), Commit.class);
        }


        if (given_branch.equals(split_point)){out.println("Given branch is an ancestor of the current branch."); return;}

        //if the file in the current branch is the same as it was at the split point then checkout that file from the given branch and stage it
        for (String s : split_point.get_files().keySet()){
            if (current_branch.get_files().containsKey(s)){
                if (split_point.compare_file(s, current_branch)){
                    if (!given_branch.get_files().containsKey(s)){
                        //if it is absent in the given branch delete and untrack it (add to removal)
                        rm(s); return;
                    }
                    checkout(given_branch, s);
                    add(s);
                }
            }
        }

        //if the given branch contains files that are ONLY present in the given branch but were not present at the split point
        for (String s : given_branch.get_files().keySet()){
            if (!split_point.get_files().containsKey(s) && !current_branch.get_files().containsKey(s)){
                checkout(given_branch, s);
                add(s);
            }
            if (current_branch.get_files().containsKey(s)){
                if (!given_branch.compare_file(s, current_branch)){
                    File current = current_branch.get_files().get(s);
                    File given = given_branch.get_files().get(s);
                    byte[] current_bytes = concat(readObject(current, byte[].class), new String("\n===\n").getBytes());
                    byte[] given_bytes = readObject(given, byte[].class);
                    current_bytes = concat(current_bytes, given_bytes);
                    Files.write(join(CWD, s).toPath(), current_bytes);
                    add(s);
                    out.println("Merged " + branchname + " into " + get_active_branch());
                    writeObject(merge, true);
                    set_active_branch(branchname);
                }
                else{
                    out.println("Encountered a merge conflict.");
                }
            }
        }
    }

    public static void checkout(String filename) throws IOException {
        Commit c = readObject(head_file, Commit.class);
        checkout(c, filename);
    }
    public static void checkout(String id, String filename) throws IOException {
        Commit c = commit_id(id);
        if (c == null){out.println("No commit with that id exists."); return;}
        checkout(c, filename);
    }
    public static void checkout_branch(String branchname) throws IOException {
        //read the head of the given branch and copy all the files into the working directory
        Head = readObject(join(GITLET_DIR, branchname), Commit.class);
        for (String s : Head.get_files().keySet()){
            byte[] bytes = readObject(Head.get_files().get(s), byte[].class);
            Path path = join(CWD, s).toPath();
            Files.write(path, bytes);
        }
        //Set the given branch as the new head
        writeObject(head_file, Head);
        //update the branch hashmap setting the given the branch as the now active branch
        @SuppressWarnings("unchecked")
        HashMap<String, Integer> branches = readObject(branch_map, HashMap.class);
        for(String s : branches.keySet()){
            branches.replace(s, 0);
        }
        branches.replace(branchname, 1);
        writeObject(branch_map, branches);

    }
    private static boolean checkout(Commit c, String filename) throws IOException {
        if(c.get_files().containsKey(filename)){
            byte[] bytes = readObject(c.get_files().get(filename), byte[].class);
            Path path = join(CWD, filename).toPath();
            Files.write(path, bytes);
            return true;
        }
        out.println("File does not exist in that commit.");
        return false;
    }
    //for finding and returning the commit with the given ID
    private static Commit commit_id(String id){
        File[] commit_files = commits.listFiles();
        for (File f : commit_files){
            if(id.compareTo(f.getName()) == 0){
                return readObject(f, Commit.class);
            }
        }
        return null;
    }
    private static void remove_tracked(Commit c){
        for (File s : CWD.listFiles()){
            if (!c.get_files().containsKey(s.getName())){
                s.delete();
            }
        }
    }

    private static byte[] concat(byte[] a, byte[] b) {
        int lenA = a.length;
        int lenB = b.length;
        byte[] c = Arrays.copyOf(a, lenA + lenB);
        System.arraycopy(b, 0, c, lenA, lenB);
        return c;
    }



    //for debugging
    public static void read_head(){
        Commit head = readObject(head_file, Commit.class);
        head.print_commit();
    }
    public static void read_master(){
        Commit to_read = readObject(master_file, Commit.class);
        to_read.print_commit();
    }
    public static void read_branch(String branchname){
        Commit to_read = readObject(join(GITLET_DIR, branchname), Commit.class);
        to_read.print_commit();
    }
    public static void read_branch_set(){
        @SuppressWarnings("unchecked")
        HashMap<String, Integer> branches = readObject(branch_map, HashMap.class);
        for (String s : branches.keySet()){
            out.println(String.format("%s %d", s, branches.get(s)));
        }
    }


}
