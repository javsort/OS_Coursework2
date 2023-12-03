import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.*;

/**
 * MemoryMgmt - class written by: Student ID - 38880237
 * 
 */
public class MemoryMgmt {
    // Memory size is 8192 bytes - 8KB

    // Variables for user input
    Integer selectedOption = 0;
    BufferedReader buffer = new BufferedReader(new InputStreamReader(System.in));
    String line = "";

    /*
     * Memory block structure:
     * List 1 size: 8 bytes
     * List 2 size: 16 bytes
     * List 3 size: 32 bytes
     * List 4 size: 64 bytes
     * List 5 size: 128 bytes
     * List 6 size: 256 bytes
     * List 7 size: 512 bytes
     * List 8 size: 1024 bytes
     * List 9 size: 2048 bytes
     * List 10 size: 4096 bytes
     * List 11 size: 8192 bytes
     */

    // Memory block lists - assign depending on size when free is called
    public List<MemoryBlock> size32 = new ArrayList<>();
    public List<MemoryBlock> size64 = new ArrayList<>();
    public List<MemoryBlock> size256 = new ArrayList<>();
    public List<MemoryBlock> size512 = new ArrayList<>();
    public List<MemoryBlock> size1024 = new ArrayList<>();
    public List<MemoryBlock> size2048 = new ArrayList<>();
    public List<MemoryBlock> size4096 = new ArrayList<>();

    // Initial Free List size 8192 bytes - 8KB
    public List<MemoryBlock> size8192 = new ArrayList<>();

    // Free list - list of free lists spread by sizes
    public List<List<MemoryBlock>> freeList = new ArrayList<>();



    public static void main(String[] args) {
        MemoryMgmt memory = new MemoryMgmt();
        memory.promptMenu();

        int trialHex = 0x0000;
        int enInteger = 0;

        for(int i = 0; i < 45; i++){
            System.out.println("Hexadecimal: " + Integer.toHexString(trialHex));
            System.out.println("Decimal: " + Integer.toHexString(enInteger));

            trialHex = trialHex + 0x0001;
            enInteger = enInteger + 1;
            
        }

        System.out.println("\nDecimal: " + trialHex);
        System.out.println("Hexadecimal: " + Integer.toHexString(trialHex));
    }

    // Implement malloc
    /*
     * Main points to follow:
     * 
     */
    public int malloc(int size){
        int pointer = 0;

        return pointer;
    }

    // Free memory - multiple free calls trigger an exception
    /*
     * Main points to follow:
     * 
     */
    public void free(int ptr){

    }

    // Return a new array of memory and a new pointer 
    /*
     * Main points to follow:
     * 
     */
    public void sbrk(int size){

    }

    public abstract class MemoryBlock{

        public int header;
        public int size;
        public int pointer;
        public boolean isFree;
        public boolean previousFree;
        public int previousSize;

        public MemoryBlock(int size, int pointer, boolean isFree, boolean previousFree, int previousSize){
            
            this.size = size;
            this.pointer = pointer;
            this.isFree = isFree;
            this.previousFree = previousFree;
            this.previousSize = previousSize;
            
        }
    }

    public class FreeBlock{
        int pointer_prev;
        int pointer_next;
        int length;

    }

    // run different tests and print results
    public void print(){

        System.out.println("Test "+ selectedOption +": HIIIII");
    }

    public void promptMenu(){
        
        while(true){
            System.out.println("\r\n==================================");
            System.out.println("          Memory Manager");
            System.out.println("  Select a problem to initialize");
            System.out.println("==================================\r\n");
            System.out.println("1. Test 1");
            System.out.println("2. Test 2");
            System.out.println("3. Test 3");
            System.out.println("0. Exit");

            System.out.print("\r\nEnter your selection: ");

            //Get user input
            try{
                line = buffer.readLine();
                selectedOption = Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Invalid selection. Please try again.");
                continue;
            } catch (IOException e) {
                System.out.println("IO Exception triggered. Bye!");
            }

            if(selectedOption == 0) {
                System.out.println("Exiting now. Bye!!!");
                break;
            }

            switch(selectedOption){
                case 1:
                    System.out.println("Test 1 initializing...");
                    // set test characteristics
                    print();
                    break;
                case 2:
                    System.out.println("Test 2 initializing...");
                    // set test characteristics
                    print();
                    break;
                case 3:
                    System.out.println("Test 3 initializing...");
                    // set test characteristics
                    print();
                    break;
                default:
                    System.out.println("Invalid selection. Bye!!!.");
                    return;
            }
        }
    }
}