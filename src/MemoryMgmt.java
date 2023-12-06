import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.BufferOverflowException;
import java.util.*;

/**
 * MemoryMgmt - class written by: Student ID - 38880237
 * 
 */
public class MemoryMgmt {
    // Memory size is 8192 bytes - 8KB

    // Variables for memory size
    Integer absoluteSize;
    Integer usableSize;

    static Integer ALLOC_HEADER = 8;
    static Integer FREE_HEADER = 16;

    
    // Variables for user input
    Integer selectedOption = 0;
    BufferedReader buffer = new BufferedReader(new InputStreamReader(System.in));
    String line = "";

    // Memory block lists - assign depending on size when free is called
    public List<MemoryBlock> size32;
    public List<MemoryBlock> size64;
    public List<MemoryBlock> size512;
    public List<MemoryBlock> size1024; 
    public List<MemoryBlock> size2048;
    public List<MemoryBlock> size4096;

    // Initial Free List size 8192 bytes - 8KB
    public List<MemoryBlock> size8192;

    // Free list - list of free lists spread by sizes
    public List<List<MemoryBlock>> freeLists;

    public List<UsedBlock> usedBlocks;

    // CHECAR COMO FUNCIONA Y SI VALE LA PENA
    public List<MemoryBlock> virtualMemory;

    // Constructor that initializes the lists to be used
    public MemoryMgmt(){
        // Initialize free lists and add them to the main list 
        size32 = new ArrayList<>();
        size64 = new ArrayList<>();
        size512 = new ArrayList<>();
        size1024 = new ArrayList<>();
        size2048 = new ArrayList<>();
        size4096 = new ArrayList<>();
        size8192 = new ArrayList<>();

        freeLists = new ArrayList<>();
        
        freeLists.add(size32);
        freeLists.add(size64);
        freeLists.add(size512);
        freeLists.add(size1024);
        freeLists.add(size2048);
        freeLists.add(size4096);
        freeLists.add(size8192);

        usedBlocks = new ArrayList<>();
    }

    // Main Method
    public static void main(String[] args) {
        MemoryMgmt memory = new MemoryMgmt();
        memory.promptMenu();

    }

    /*
     * malloc Implementation
     * Main points to follow:
     * allocate (size)
     * return a pointer to the start of requested memory
     * TOTAL MEMORY IS 8192 BYTES REMEMBER
     *  - After reaching 8192 bytes, no more memory can be allocated, call sbrk
     * Try to allocate through Best Fit
     */
    public int malloc(int size){
        System.out.print("Requesting " + size + " bytes of memory... ");

        // Get free slot to partition memory from 
        FreeBlock toUse = bestFit(size);            // Check if any free block is available, and choose the one to be used, else call sbrk
        
        // if null, call sbrk
        if(toUse == null){
            throw new OutOfMemoryError("Not enough memory to allocate. Call sbrk. (TO BE IMPLEMENTED)");
        }

        // If yes, allocate through best fit
        UsedBlock Allocated = new UsedBlock(toUse.previousFree, toUse.previousSize, false, size);

        // Update free block
        toUse.previousFree = false;
        toUse.previousSize += Allocated.totalsize;
        toUse.size = toUse.size - Allocated.size;

        // Update free list sizes
        absoluteSize = absoluteSize - Allocated.totalsize;
        usableSize = usableSize - Allocated.size;

        // Add to free list
        addBackToFreeList(toUse);

        // String hex = String.format("0x%04x", Allocated.pointerReturned);

        System.out.print("memory successfully allocated at " + getMeminHex(Allocated.pointerReturned) + ".\r\n\n");

        return Allocated.pointerReturned;
    }


    // Best fit
    public FreeBlock bestFit(int size){

        // Sort free lists
        freeLists.sort((list1, list2) -> {
            if (list1.isEmpty()) {
                return (list2.isEmpty()) ? 0 : -1;
            }
            if (list2.isEmpty()) {
                return 1;
            }
            return Integer.compare(list1.get(0).getSize(), list2.get(0).getSize());
        });

        for(List<MemoryBlock> list : freeLists){
            if(!list.isEmpty() && list.get(0).getSize() >= size){

                // return the first freeblock that fits the size
                return (FreeBlock) list.remove(0);
            }
        }

        // MEMORY IS FULL
        return null;
    }

    public void addBackToFreeList(FreeBlock toAdd){
        if(toAdd.getSize() < 32){
            size32.add(toAdd);
        } else if(toAdd.getSize() < 64){
            size64.add(toAdd);
        } else if(toAdd.getSize() < 512){
            size512.add(toAdd);
        } else if(toAdd.getSize() < 1024){
            size1024.add(toAdd);
        } else if(toAdd.getSize() < 2048){
            size2048.add(toAdd);
        } else if(toAdd.getSize() < 4096){
            size4096.add(toAdd);
        } else if(toAdd.getSize() < 8192){
            size8192.add(toAdd);
        }
    }

    /*
     * Free Implementation
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
        // Check if pointer is valid


        // Check if pointer is already free

        // Check for upper and lower bounds

        // Sort free blocks
    }

    /*
     * sbrk Implementation
     * Main points to follow:
     * return a new array of memory with its starting pointer
     * new array has to be the smallest power of 2 that can fit the size
     * Every new chunk isn't strictly contiguous
     */
    public void sbrk(int size){

        
    }

    /*
     * Coalesce Implementation
     * Main points to follow:
     * Coalesce free blocks whether it:
     *  - has a free block before
     *  - has a free block after
     *  - has a free block before and after
     * And update free list
     * 
     * If no free block before or after, just add to free list
     * Return nothing
     */
    public void coalesce(){
            
        }

    /*
     * MemoryBlock Abstract Class
     * to be extended by FreeBlock and UsedBlock
     */
    public abstract class MemoryBlock{
        int previousSize;
        boolean previousFree;

        int size;
        boolean isFree;
        int pointerToReturn;

        int sizeHeader; // can be +8 or +16 depending on free or used
        int totalsize; // the size of the block + the size of the header

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

        public int getSize(){
            return this.size;
        }
    }

    /*
     * FreeBlock Class
     */

    public class FreeBlock extends MemoryBlock {
        // USE FREE_HEADER = 16

        public FreeBlock next;
        public FreeBlock prev;

        public FreeBlock(boolean previousFree, int previousSize, boolean isFree, int size, FreeBlock pointer_prev, FreeBlock pointer_next){
            super(FREE_HEADER, previousFree, previousSize, isFree, size);
            this.prev = pointer_prev;
            this.next = pointer_next;
            this.totalsize = size - FREE_HEADER;
        }

        public int getSize(){
            return this.size;
        }

    }

    /*
     * UsedBlock Class
     */
    public class UsedBlock extends MemoryBlock {
        // USE ALLOC_HEADER = 8
        public int assignedSp[];

        public int pointerReturned;

        public UsedBlock(boolean previousFree, int previousSize, boolean isFree, int size){
            super(ALLOC_HEADER, previousFree, previousSize, isFree, size);

            this.pointerReturned = previousSize + ALLOC_HEADER;

            this.totalsize = size + ALLOC_HEADER;
            this.assignedSp = new int[size];

            usedBlocks.add(this);
        }

        public boolean inputString(String dataString){
            // Get size of string in ASCII - considering each ASCII character is 2 bytes
            char[] toStore = dataString.toCharArray();

            int sizeInBytes = (toStore.length + 1) * 2; // +1 for null terminating char, *2 for 2 bytes per char

            try {
                if(sizeInBytes < size){
                    // Store string in the assigned space
                    for(int i = 0; i < toStore.length; i++){
                        assignedSp[i] = (int) toStore[i];
                    }
                    assignedSp[toStore.length] = (int) '\0'; // Null terminating char

                    // Update space and pointer
                    size = size - assignedSp.length;
                    pointerReturned = pointerReturned + assignedSp.length;

                    return true;
                } else {
                    System.out.println("String is too big to fit in the allocated memory.");
                    throw new BufferOverflowException();

                }
            } catch (BufferOverflowException e) {
                System.out.println("Buffer Overflow Exception triggered. Please try again.");
                Thread.currentThread().interrupt();
                return false;
            }
        }

        public String getStoredString(){
            String toReturn = "";

            for(int i = 0; i < assignedSp.length; i++){
                toReturn += (char) assignedSp[i];
            }

            return toReturn;
        }

        // to fill!
        public int getDataInt(){
            return this.assignedSp[0];
        }

        public int getPointer(){
            return this.pointerReturned;
        }
    } 

    // run different tests and print results
    public void print(){
        switch (selectedOption) {
            case 1:
                int pointer;
                pointer = malloc(28);

                pointer = inputDataString("Testing 1", pointer);

                if(pointer < 0){
                    break;
                }
                
                try{
                    System.out.println("Sleeping for 1 second... to simulate cool computing stuff. Think ab ur day for a second :)\n");
                    Thread.sleep(1000);
                } catch (InterruptedException e) {

                }

                retrieveString(pointer);

                free(pointer);
                break;
        
            default:
                System.out.println("Invalid selection. Unknown Test choice.");
                break;
        }
    }

    public int inputDataString(String dataString, int pointer){
        System.out.println("Storing the `" + dataString + "` string! At: " + getMeminHex(pointer) + " Please wait...\n");

        for(UsedBlock current : usedBlocks){
            if(current.pointerReturned == pointer){
                if(current.inputString(dataString)){
                    return current.pointerReturned;
                } else {
                    return -1;
                }
            }
        }

        System.out.println("Pointer not found. Please try again.");
        return -1;
    }

    public void retrieveString(int pointer){
        System.out.println("Retrieving data from pointer: " + getMeminHex(pointer) + ". Please wait...\n");

        for(UsedBlock current : usedBlocks){
            if(current.pointerReturned == pointer){
                String storedString = current.getStoredString();
                System.out.println("String retrieved: '" + storedString + "'\n");
                return;
            }
        }

        System.out.println("Pointer not found. Please try again.");
    }



    /*
     * Prompt Menu
     * Prompt user to select a test to run
     */
    public void promptMenu(){
        while(true){
            cleanMemory();

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
                    System.out.println("Test 1 initializing...\n");
                    

                    print();
                    break;
                case 2:
                    System.out.println("Test 2 initializing...\n");
                    // set test characteristics
                    print();
                    break;
                case 3:
                    System.out.println("Test 3 initializing...\n");
                    // set test characteristics
                    print();
                    break;
                default:
                    System.out.println("Invalid selection. Bye!!!.\n");
                    return;
            }
        }
    }

    public void cleanMemory(){
        // Clear all lists
        for(int i = 0; i < freeLists.size(); i++){
            freeLists.get(i).clear();
        }

        // Clear used blocks
        usedBlocks.clear();

        // Variables for memory size
        absoluteSize = 8192;
        usableSize = 8192;

        // Initial full free block
        FreeBlock OGfreeBlock = new FreeBlock(false, 0, true, 8192, null, null);

        // Add the original free block to the 8KB list
        size8192.add(OGfreeBlock);
    }

    public String getMeminHex(int pointer){
        String hex = String.format("0x%04x", pointer);
        return hex;
    }
}