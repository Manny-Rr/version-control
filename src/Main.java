package src;
import java.io.IOException;
import static java.lang.System.out;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Manraj
 */
public class Main {


    public static void main(String[] args) throws IOException{

        int length = args.length;
        
        if (length == 0) out.println("Please enter a command.");
        if (args[1].compareTo("init") != 0){
            if (!Repository.GITLET_DIR.exists())
                out.println("Not in an initialized Gitlet directory.");
        }

        String firstArg = args[0];
        switch(firstArg) {

            case "init":
                if (length > 1) {out.println("Incorrect operands."); break;}
                Repository.init();
                break;


            case "add":
                try{
                    Repository.add(args[1]);
                    break;
                } catch (IndexOutOfBoundsException e){
                    out.println("Incorrect operands."); break;
                }
                
                
            case "commit":
                if (length > 2) {out.println("Incorrect operands.");break;}
                try{
                    Repository.commit(args[1]);
                    break;
                } catch (IndexOutOfBoundsException e){
                    out.println("Please enter a commit message"); break;
                }
                
            case "rm":
                try{
                    Repository.rm(args[1]);
                    break;
                } catch (IndexOutOfBoundsException e){
                    out.println("Incorrect operands."); break;
                }


            case "log":
                if (length > 1) {out.println("Incorrect operands."); break;}
                Repository.log();
                break;

            case "global-log":
                if (length > 1) {out.println("Incorrect operands."); break;}
                Repository.global_log();
                break;

            case "find":
                try{
                    Repository.find(args[1]);
                    break;
                } catch (IndexOutOfBoundsException e){
                    out.println("Incorrect operands."); break;
                }

                
            case "status":
                if (length > 1) {out.println("Incorrect operands."); break;}
                Repository.status();
                break;


            case "checkout":
                if (length > 4 || length < 2) {out.println("Incorrect operands."); break;}
                switch (length){
                    case 2:
                        Repository.checkout_branch(args[1]); break;
                        
                    case 3:
                        Repository.checkout(args[2]); break;
                        
                    case 4:
                        Repository.checkout(args[1], args[3]); break;
                }


            case "branch":
                try{
                    Repository.branch(args[1]);
                    break;
                } catch (IndexOutOfBoundsException e){
                    out.println("Incorrect operands."); break;
                }

            case "rm-branch":
                try{
                    Repository.remove_branch(args[1]);
                    break;
                } catch (IndexOutOfBoundsException e){
                    out.println("Incorrect operands."); break;
                }

            case "reset":
                try{
                    Repository.reset(args[1]);
                    break;
                } catch (IndexOutOfBoundsException e){
                    out.println("Incorrect operands."); break;
                }

                
            case "merge":
                try{
                    Repository.merge(args[1]);
                    break;
                } catch (IndexOutOfBoundsException e){
                    out.println("Incorrect operands."); break;
                }
                

            //for reading the head commit
            case "read":

                if (args[1].compareTo("head") == 0){
                    Repository.read_head();
                    break;
                }
                else if (args[1].compareTo("master") == 0){
                    Repository.read_master();
                    break;
                }
                else if (args[1].compareTo("set") == 0){
                    Repository.read_branch_set();
                    break;
                }
                else{
                    Repository.read_branch(args[1]);
                    break;
                }

        }
        out.println("No command with that name exists.");
        System.exit(0);
    }
}
