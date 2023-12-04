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

    // Variables for memory size
    Integer totalSize = 8192;
    Integer freeSize = 8192;

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

        

        for(int i = 0; i < 45; i++){
            
        }

    }

    // Implement malloc
    /*
     * Main points to follow:
     * allocate (size)
     * return a pointer to the start of requested memory
     * TOTAL MEMORY IS 8192 BYTES REMEMBER
     *  - After reaching 8192 bytes, no more memory can be allocated, call sbrk
     * Try to allocate through Best Fit
     */
    public int malloc(int size){
        UsedBlock Allocated = new UsedBlock(false, 0, false, size);
        
        if(Allocated.totalsize > freeSize){
            System.out.println("Not enough memory to allocate. Call sbrk. (TO BE IMPLEMENTED)");
            return -1;
        }

        freeSize = freeSize - Allocated.totalsize; 


        return Allocated.pointerReturned;
    }

    // Free memory - multiple free calls trigger an exception
    /*
     * Main points to follow:
     * Free memory chunk from the usedblock
     * Coalesce free blocks whether it:
     *  - has a free block before
     *  - has a free block after
     *  - has a free block before and after
     * And update free list
     * 
     * If no free block before or after, just add to free list
     * Multiple free calls on the same pointer == exception
     * Return nothing
     */
    public void free(int ptr){

    }

    // Return a new array of memory and a new pointer 
    /*
     * Main points to follow:
     * return a new array of memory
     * new array has to be the smallest power of 2 that can fit the size
     * Every new chunk isn't strictly contiguous
     */
    public void sbrk(int size){

    }

    public abstract class MemoryBlock{
        int previousSize;
        boolean previousFree;

        int size;
        boolean isFree;
        int pointerToReturn;

        int sizeHeader; 
        int totalsize;// size + 8 for the header

        int pointer_prev;
        int pointer_next;

        public MemoryBlock(int sizeHeader, boolean previousFree, int previousSize, boolean isFree, int size){
            this.previousFree = previousFree;
            this.previousSize = previousSize;

            this.sizeHeader = sizeHeader;

            this.isFree = isFree;

            if(size < 0){
                this.size = 0;
                this.totalsize = sizeHeader + size;
                System.out.println("Size cannot be negative. Set to 0 to avoid unexpected behavior.");
            } else {
                this.size = size;
                this.totalsize = sizeHeader + size;
            }
        }
    }

    public class FreeBlock extends MemoryBlock {
        private static int HEADER = 16;

        public FreeBlock next;
        public FreeBlock prev;

        public FreeBlock(boolean previousFree, int previousSize, boolean isFree, int size, FreeBlock pointer_prev, FreeBlock pointer_next){
            super(HEADER, previousFree, previousSize, isFree, size);
            this.prev = pointer_prev;
            this.next = pointer_next;
        }

        public int getSize(){
            return this.size;
        }

    }

    public class UsedBlock extends MemoryBlock {
        private static int HEADER = 8;

        public int assignedSp[];

        public int pointerReturned;

        public UsedBlock(boolean previousFree, int previousSize, boolean isFree, int size){
            super(HEADER, previousFree, previousSize, isFree, size);

            this.pointerReturned = previousSize + HEADER;

            this.assignedSp = new int[size];
        }

        /*public boolean fillMemory(String dataString){
            // Get size of string in UTF-8 encoding
            byte[] toUTF8 = dataString.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            int stringInBytes = toUTF8.length;

            if(toUTF8.length > assignedSp.length){
                System.out.println("String is too big to fit in memory block.");
                return false;
            }

            return true;
        }*/
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