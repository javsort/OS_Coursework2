import java.io.*;
import java.nio.BufferOverflowException;
import java.util.*;

/**
 * MemoryMgmt - class written by: Student ID - 38880237
 * 
 */
public class MemoryMgmt {
    // Memory size is 8192 bytes - 8KB
    static final Integer memorySize = 8192;

    // Static integers for each of the block's headers
    static Integer ALLOC_HEADER = 8;
    static Integer FREE_HEADER = 16;

    // Int to be used as pointer
    public int pointer;
    
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

    // Active Virtual Memory
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

        // Initialize main freeList
        freeLists = new ArrayList<>();
        
        freeLists.add(size32);
        freeLists.add(size64);
        freeLists.add(size512);
        freeLists.add(size1024);
        freeLists.add(size2048);
        freeLists.add(size4096);
        freeLists.add(size8192);

        // Start virtual memory
        virtualMemory = new ArrayList<>();

        // Start usedBlocks
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
        
        // Sleep for cool GUI computing efx purposes
        guiEFX();

        // Get free slot to partition memory from 
        FreeBlock toUse = bestFit(size);            // Check if any free block is available, and choose the one to be used, else call sbrk
        
        // if null, means there's no more space. Call sbrk
        if(toUse.totalsize < size || toUse == null){
            throw new OutOfMemoryError("Not enough memory to allocate. Call sbrk. (TO BE IMPLEMENTED)");
        }

        // If yes, allocate through best fit
        UsedBlock Allocated = new UsedBlock(toUse.previousFree, toUse.previousSize, false, size);

        // Update free block
        toUse.previousFree = false;
        toUse.previousSize += Allocated.size;
        toUse.size = toUse.size - Allocated.size;
        toUse.totalsize -= Allocated.totalsize;  

        // Update used block
        Allocated.startAddress = toUse.getStartAddress();
        Allocated.endAddress = Allocated.pointerReturned + size;
        Allocated.pointerReturned = Allocated.getStartAddress() + ALLOC_HEADER;

        // Update free list address
        toUse.startAddress = Allocated.getEndAddress() + 1;

        // Add free list used back to free list
        addBackToFreeList(toUse);

        System.out.print("memory successfully allocated. \nPointer: " + getMeminHex(Allocated.getPointerToWrite()) + ".\r\n\n");

        // Sort virtual memory by pointers
        sortMemory();

        // Returned pointer allocated
        return Allocated.getPointerToWrite();
    }

    // Method to sort free lists by size
    public void sortLists(){
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
    }


    // Best fit - find the most appropiate free list for the requested size
    public FreeBlock bestFit(int size){
        sortLists();
        
        for(List<MemoryBlock> list : freeLists){
            if(!list.isEmpty() && list.get(0).size >= size){

                // return the first freeblock that fits the size
                return (FreeBlock) list.remove(0);
            }
        }

        // MEMORY IS FULL - CALL SBRK WHEN BACK
        return null;
    }

    // Find the most appropiate size for the updated free block
    public void addBackToFreeList(FreeBlock toAdd){
        if(toAdd.getSize() < 32){
            size32.add(toAdd);
            //System.out.println("Free List added to size 32. Total Size: " + toAdd.totalsize);
        } else if(toAdd.getSize() < 64){
            size64.add(toAdd);
            //System.out.println("Free List added to size 64. Total Size: " + toAdd.totalsize);
        } else if(toAdd.getSize() < 512){
            size512.add(toAdd);
            //System.out.println("Free List added to size 512. Total Size: " + toAdd.totalsize);
        } else if(toAdd.getSize() < 1024){
            size1024.add(toAdd);
            //System.out.println("Free List added to size 1024. Total Size: " + toAdd.totalsize);
        } else if(toAdd.getSize() < 2048){
            size2048.add(toAdd);
            //System.out.println("Free List added to size 2048. Total Size: " + toAdd.totalsize);
        } else if(toAdd.getSize() < 4096){
            size4096.add(toAdd);
            //System.out.println("Free List added to size 4096. Total Size: " + toAdd.totalsize);
        } else if(toAdd.getSize() < 8192){
            size8192.add(toAdd);
            //System.out.println("Free List added to size 8192. Total Size: " + toAdd.totalsize);
        }
    }

    // Check if pointer is in memory, send an exception if not
    public boolean isInMemory(int ptr){
        for(MemoryBlock current : virtualMemory){
            if(current instanceof UsedBlock){
                UsedBlock toCheck = (UsedBlock) current;

                if(toCheck.getPointerToWrite() == ptr){
                    return true;
                }
            }
        }

        return false;
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
        System.out.print("Freeing pointer: " + getMeminHex(ptr) + "...");

        // Sleep for cool GUI computing efx purposes
        guiEFX();

        // Check if pointer is valid
        if(ptr < 0){
            System.out.println("Invalid pointer. Please try again.");
            return;
        }	

        // Blocks to be used
        UsedBlock current = null;
        FreeBlock newFreeBlock = null;

        // Throw exception if ptr is not in memory or user is calling free again
        try {
            if(!isInMemory(ptr)){
                throw new Exception();
            }
        } catch (Exception e) {
            System.out.println(" Exception triggered in thread. Exiting.");
            return;
        }

        // Check if pointer is used in memory
        for(int i = 0; i < virtualMemory.size(); i++){

            // If its a used block, check if pointer matches
            if(virtualMemory.get(i) instanceof UsedBlock){
                current = (UsedBlock) virtualMemory.get(i);

                // If pointer matches, free it
                if(current.getPointerToWrite() == ptr && !current.isFree){
                    current.assignedSp = null;
                    current.size = current.originalSize;
                    current.isFree = true;

                    // Update memory
                    newFreeBlock = updateMemory();

                    break;
                }
            } else {
                continue;
            }

            
        }

        // Update free list (Coalesce)
        updateFreeList(newFreeBlock);

        System.out.print(" memory successfully freed.\r\n\n");
    }

    // Update memory after freeing a block
    public FreeBlock updateMemory(){
        MemoryBlock current;
        UsedBlock toFree = null;


        // Find the block to free
        for(int i = 0; i < virtualMemory.size(); i++){
            current = virtualMemory.get(i);

            if(current instanceof UsedBlock && current.isFree){
                toFree = (UsedBlock) virtualMemory.get(i);

                break;
            }
        }

        // Remove the used block to free from memory
        virtualMemory.remove(toFree);
        usedBlocks.remove(toFree);

        // Reset current
        current = null;

        // Create new free block current for checking
        FreeBlock freeCurrent;

        // Create new free block to add to memory
        FreeBlock newFree = new FreeBlock(toFree.previousFree, toFree.previousSize, true, toFree.totalsize, null, null);

        // Update addresses
        newFree.startAddress = toFree.getStartAddress();
        newFree.endAddress = toFree.getEndAddress();

        // Check and update previous and next pointers
        for(int i = 0; i < virtualMemory.size(); i++){
            current = virtualMemory.get(i);

            // Update implicit free list
            if(current instanceof FreeBlock){
                freeCurrent = (FreeBlock) current;

                // Means it's in the middle of two free blocks - update for lower freeSlot
                if(freeCurrent.next != null && freeCurrent.next.getStartAddress() > toFree.getStartAddress() && !freeCurrent.equals(newFree)){
                    freeCurrent.setNext(newFree);
                    newFree.setPrevious(freeCurrent);

                    continue;
                }

                // Means it's in the middle of two free blocks as well - update for higher freeSlot
                if(freeCurrent.prev != null && freeCurrent.prev.getStartAddress() < toFree.getStartAddress() && !freeCurrent.equals(newFree)){
                    freeCurrent.setPrevious(newFree);
                    newFree.setNext(freeCurrent);

                    continue;
                }

                // Means it's the last one and a new one is being added after
                if(freeCurrent.next == null && freeCurrent.getStartAddress() < toFree.getStartAddress() && !freeCurrent.equals(newFree)){
                    freeCurrent.setNext(newFree);
                    newFree.setPrevious(freeCurrent);
                    continue;
                }

                // Means it's the first one and a new one is being added before
                if(freeCurrent.prev == null && freeCurrent.getStartAddress() > toFree.getStartAddress() && !freeCurrent.equals(newFree)){
                    freeCurrent.setPrevious(newFree);
                    newFree.setNext(freeCurrent);
                    continue;
                }

                continue;
            }
        }
        return newFree;
    }

    // Set coalescing conditions
    public void updateFreeList(FreeBlock newFree){
        // Sort lists
        sortLists();
        sortMemory();

        // Check if there's a free block to the right or left
        boolean coalesceToRight = false;
        boolean coalesceToLeft = false;

        // Check if there's a free block to the right
        if(newFree.next != null && newFree.next.getStartAddress() == newFree.getEndAddress() + 1){
            coalesceToRight = true;
        }
        
        // Check if there's a free block to the left
        if(newFree.prev != null && newFree.prev.getEndAddress() == newFree.getStartAddress() - 1){
            coalesceToLeft = true;
        }

        // Coalesce depending on the case
        if(coalesceToRight && coalesceToLeft){
            // Coalesce to the right and left
            coalesce(newFree, 3);

        } else if (coalesceToRight){
            // Coalesce to the right
            coalesce(newFree, 2);

        } else if (coalesceToLeft){
            // Coalesce to the left
            coalesce(newFree, 1);

        }
    }

    // Coalesce free blocks depending on conditions
    public void coalesce(FreeBlock sent,  int coalesce){
        // new freeBlock
        FreeBlock toConnect = sent;
        removeFromFreeLists(toConnect);

        // Free blocks to connect
        FreeBlock onLeft;
        FreeBlock onRight;

        // new final coalesced block to be added
        FreeBlock joinedBlock;

        // Coalesce depending on conditions
        switch(coalesce){

            // TO THE LEFT - | OLD FREE BLOCK | NEW FREE BLOCK |
            case 1:
                onLeft = toConnect.prev;

                // Remove from active memory
                removeFromFreeLists(onLeft);
                virtualMemory.remove(toConnect);
                virtualMemory.remove(onLeft);

                joinedBlock = new FreeBlock(onLeft.previousFree, onLeft.previousSize, true, toConnect.getEndAddress() - onLeft.getStartAddress(), onLeft.prev, toConnect.next);

                // Update addresses
                joinedBlock.startAddress = onLeft.getStartAddress();
                joinedBlock.endAddress = toConnect.getEndAddress();

                // Update pointers
                addBackToFreeList(joinedBlock);
                break;

            // TO THE RIGHT - | NEW FREE BLOCK | OLD FREE BLOCK |
            case 2:
                onRight = toConnect.next;

                // Remove from active memory
                removeFromFreeLists(onRight);
                virtualMemory.remove(toConnect);
                virtualMemory.remove(onRight);

                joinedBlock = new FreeBlock(toConnect.previousFree, toConnect.previousSize, true, onRight.getEndAddress() - toConnect.getStartAddress(), toConnect.prev, onRight.next);

                // Update addresses
                joinedBlock.startAddress = toConnect.getStartAddress();
                joinedBlock.endAddress = onRight.getEndAddress();

                // Update pointers
                addBackToFreeList(joinedBlock);
                break;

            // TO THE LEFT AND RIGHT - | OLD FREE BLOCK 1 | NEW FREE BLOCK | OLD FREE BLOCK 2 |
            case 3:
                onLeft = toConnect.prev;
                onRight = toConnect.next;

                removeFromFreeLists(onRight);
                removeFromFreeLists(onLeft);

                virtualMemory.remove(toConnect);
                virtualMemory.remove(onRight);
                virtualMemory.remove(onLeft);

                joinedBlock = new FreeBlock(onLeft.previousFree, onLeft.previousSize, true, onRight.getEndAddress() - onLeft.getStartAddress(), onLeft.prev, onRight.next);

                // Update addresses
                joinedBlock.startAddress = onLeft.getStartAddress();
                joinedBlock.endAddress = onRight.getEndAddress();

                // Update pointers
                addBackToFreeList(joinedBlock);
                break;

            default:
                System.out.println("How did u get here????");

                break;
        }
    }

    // Remove free block from free lists
    public void removeFromFreeLists(FreeBlock toRemove){
        for(List<MemoryBlock> list : freeLists){
            if(list.contains(toRemove)){
                list.remove(toRemove);
            }
        }
    }

    // Sort memory by start address
    public void sortMemory(){
        Collections.sort(virtualMemory, new Comparator<MemoryBlock>(){
            @Override
            public int compare(MemoryBlock block1, MemoryBlock block2) {
                return Integer.compare(block1.getStartAddress(), block2.getStartAddress());
            }
        });
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
     * MemoryBlock Abstract Class
     * to be extended by FreeBlock and UsedBlock
     */
    public abstract class MemoryBlock{
        int previousSize;
        boolean previousFree;

        int size;
        int originalSize;

        boolean isFree;
        int pointerToReturn;

        int sizeHeader; // can be +8 or +16 depending on free or used
        int totalsize; // the size of the block + the size of the header

        int pointer_prev;
        int pointer_next;

        int startAddress;
        int endAddress;

        public MemoryBlock(int sizeHeader, boolean previousFree, int previousSize, boolean isFree, int size){
            this.previousFree = previousFree;
            this.previousSize = previousSize;

            this.sizeHeader = sizeHeader;

            this.isFree = isFree;

            if(size < 0){
                this.size = 0;
                this.originalSize = size;
                this.totalsize = sizeHeader + size;
                System.out.println("Size cannot be negative. Set to 0 to avoid unexpected behavior.");
            } else {
                this.size = size;
                this.originalSize = size;
                this.totalsize = sizeHeader + size;
            }
        }

        public int getSize(){
            return this.size;
        }

        public int getAbsoluteSize(){
            return this.totalsize;
        }

        public int getStartAddress(){
            return this.startAddress;
        }

        public int getEndAddress(){
            return this.endAddress;
        }

        public int getPointer(){
            return this.pointerToReturn;
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

            this.startAddress = previousSize;
            this.endAddress = previousSize + totalsize;

            virtualMemory.add(this);
        }

        public void setPrevious(FreeBlock prev){
            this.prev = prev;
        }

        public void setNext(FreeBlock next){
            this.next = next;
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
            virtualMemory.add(this);
        }


        // Store string in the assigned space
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

        // Retrieve string from the assigned space
        public String getStoredString(){
            String toReturn = "";

            for(int i = 0; i < assignedSp.length; i++){
                toReturn += (char) assignedSp[i];
            }

            return toReturn;
        }


        // Store int in the assigned space
        public boolean inputInt(int dataInt){
            int i = 0;

            while(i < assignedSp.length && assignedSp[i] != 0){
                i++;
            }

            try{
                if(i < assignedSp.length) {
                    assignedSp[i] = dataInt;

                    // Ints are 4 bytes long, so account in memory for that
                    size = size - Integer.BYTES;
                    pointerReturned = pointerReturned + assignedSp.length;

                    return true;
                } else {
                    System.out.println("No more space to store data.");
                    throw new BufferOverflowException();
                }
            } catch (BufferOverflowException e) {
                System.out.println("Buffer Overflow Exception triggered. Please try again.");
                Thread.currentThread().interrupt();
                return false;
            }
        }


        // Retrieve int from the assigned space
        public int getStoredInt(){
            for(int i = assignedSp.length - 1 ; i >= 0; i--){
                if(assignedSp[i] != 0){
                    // Return int in last occupied position in assignedSpace
                    return assignedSp[i];
                }
            }

            return 0;
        }

        public int getPointerToWrite(){
            return this.pointerReturned;
        }
    } 

    // run different tests and print results
    public void print(){
        int ptr1 = 0;
        int ptr2 = 0;
        int ptr3 = 0;
        int ptr4 = 0;

        switch (selectedOption) {
            // Malloc, store String, get String, free
            case 1:
                pointer = malloc(28);

                pointer = inputDataString("Testing 1", pointer);

                if(pointer < 0){
                    break;
                }

                retrieveString(pointer);

                free(pointer);

                // Sleep for cool GUI computing efx purposes
                guiEFX();

                break;

            // Malloc ptr1, malloc ptr2, malloc ptr3, free ptr2, malloc ptr4, free all
            case 2:
                ptr1 = malloc(28);
                ptr2 = malloc(1024);
                ptr3 = malloc(28);

                free(ptr2);
                
                ptr4 = malloc(512);

                free(ptr1);
                free(ptr3);
                free(ptr4);

                // Sleep for cool GUI computing efx purposes
                guiEFX();

                break;

            // Malloc 7168, malloc 1024, free all - supposed to call sbrk
            case 3:
                ptr1 = malloc(7168);
                ptr2 = malloc(1024);

                free(ptr1);
                free(ptr2);

                break;

            // Malloc 1024, malloc 28, free 28, free 28 again
            case 4:
                ptr1 = malloc(1024);
                ptr2 = malloc(28);
                
                free(ptr2);
                free(ptr2);

                // Sleep for cool GUI computing efx purposes
                guiEFX();

                break;

            // Store int test
            case 5: 
                pointer = malloc(1024);

                pointer = inputDataInt(728, pointer);
                

                if(pointer < 0){
                    break;
                }

                retrieveInt(pointer);

                free(pointer);

                // Sleep for cool GUI computing efx purposes
                try{ 
                    Thread.sleep(750);
                } catch (InterruptedException e) {

                }

                break;
                
            default:
                System.out.println("Invalid selection. Unknown Test choice.");
                break;
        }
    }

    // Print current memory
    public void printCurrentMemory(){
        sortMemory();

        UsedBlock toPrint;
        FreeBlock toPrintFree;

        for(MemoryBlock list : virtualMemory){
            if(list instanceof FreeBlock){
                toPrintFree = (FreeBlock) list;
                System.out.println("Free block of size " + toPrintFree.getSize() + " at " + getMeminHex(toPrintFree.getStartAddress()) + " to " + getMeminHex(toPrintFree.getEndAddress()));
            } else {
                toPrint = (UsedBlock) list;
                System.out.println("Used block of size " + toPrint.getSize() + " at " + getMeminHex(toPrint.getStartAddress()) + " to " + getMeminHex(toPrint.getEndAddress()) + " with pointer " + getMeminHex(toPrint.getPointerToWrite()));
            }
        }
    }

    // Print current memory of a used chunk
    public void printCurrentState(int pointer){
        for(MemoryBlock checking : virtualMemory){
            if(checking instanceof UsedBlock){
                UsedBlock current = (UsedBlock) checking;
                for(int i = 0; i < current.assignedSp.length; i++){
                    if(current.assignedSp[i] != 0){
                        System.out.println("Current space at " + i + " is: " + current.assignedSp[i]);
                    }
                }
            }
        }
    }

    // Input a string in memory
    public int inputDataString(String dataString, int pointer){
        System.out.println("Storing the `" + dataString + "` string at: " + getMeminHex(pointer) + "! Please wait...\n");


        // Sleep for cool GUI computing efx purposes
        guiEFX();

        for(MemoryBlock checking : virtualMemory){
            if(checking instanceof UsedBlock){
                UsedBlock current = (UsedBlock) checking;
                if(current.getPointerToWrite() == pointer){
                    if(current.inputString(dataString)){
                        return current.getPointerToWrite();
                    } else {
                        return -1;
                    }
                }
            }
        }

        System.out.println("Pointer not found. Please try again.");
        return -1;
    }

    // Retrieve a string from memory
    public void retrieveString(int pointer){
        System.out.println("Retrieving data from pointer: " + getMeminHex(pointer) + ". Please wait...\n");

        // Sleep for cool GUI computing efx purposes
        guiEFX();

        for(MemoryBlock checking : virtualMemory){
            if(checking instanceof UsedBlock){
                UsedBlock current = (UsedBlock) checking;
                if(current.getPointerToWrite() == pointer){
                    String storedString = current.getStoredString();
                    System.out.println("String retrieved: '" + storedString + "'\n");
                    return;
                }
            }
        }
        
        System.out.println("Pointer not found. Please try again.");
    }


    // Input an int in memory
    public int inputDataInt(int data, int pointer){
        System.out.println("Storing the `" + data + "` int at: " + getMeminHex(pointer) + "! Please wait...\n");

        // Sleep for cool GUI computing efx purposes
        guiEFX();

        for(MemoryBlock checking : virtualMemory){
            if(checking instanceof UsedBlock){
                UsedBlock current = (UsedBlock) checking;
                if(current.getPointerToWrite() == pointer){
                    if(current.inputInt(data)){
                        return current.getPointerToWrite();
                    } else {
                        return -1;
                    }
                }
            }
        }

        System.out.println("Pointer not found. Please try again.");
        return -1;
    }


    // Retrieve an int from memory
    public void retrieveInt(int pointer){
        System.out.println("Retrieving data from pointer: " + getMeminHex(pointer) + ". Please wait...\n");

        // Sleep for cool GUI computing efx purposes
        guiEFX();

        for(MemoryBlock checking : virtualMemory){
            if(checking instanceof UsedBlock){
                UsedBlock current = (UsedBlock) checking;
                if(current.getPointerToWrite() == pointer){
                    int storedInt = current.getStoredInt();
                    System.out.println("Int retrieved: '" + storedInt + "'\n");
                    return;
                }
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

            System.out.println("\r\n====================================");
            System.out.println("           Memory Manager");
            System.out.println("      By Student ID - 38880237\n");
            System.out.println("   Select a problem to initialize");
            System.out.println("====================================\r\n");
            System.out.println("1. Test 1 - Request 28 bytes -> store a String -> retrieve String -> free");
            System.out.println("2. Test 2 - Request 28 bytes -> request 1024 bytes -> request 28 bytes -> free 1024 bytes -> request 512 bytes -> free all");
            System.out.println("3. Test 3 - Request 7168 bytes -> request 1024 bytes -> free all");
            System.out.println("4. Test 4 - Request 1024 bytes -> request 28 bytes -> free 28 bytes -> free 28 bytes again");
            System.out.println("5. Test 5 - Request 1024 bytes -> store an int -> retrieve int -> free");
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
                    print();
                    break;

                case 3:
                    System.out.println("Test 3 initializing...\n");
                    print();
                    break;

                case 4: 
                    System.out.println("Test 4 initializing...\n");
                    print();
                    break;

                case 5: 
                    System.out.println("Test 5 initializing...\n");
                    print();
                    break;

                default:
                    System.out.println("Invalid selection. Bye!!!.\n");
                    return;
            }
        }
    }

    // Clean memory
    public void cleanMemory(){
        // Clear all lists
        for(int i = 0; i < freeLists.size(); i++){
            freeLists.get(i).clear();
        }

        // Reset pointer
        pointer = 0;

        // Clear used blocks
        usedBlocks.clear();
        virtualMemory.clear();

        // Initial full free block
        FreeBlock OGfreeBlock = new FreeBlock(false, 0, true, 8192, null, null);

        // Add the original free block to the 8KB list
        size8192.add(OGfreeBlock);
    }

    // Get memory in hex for printing
    public String getMeminHex(int pointer){
        String hex = String.format("0x%04x", pointer);
        return hex;
    }

    // Sleep for cool GUI computing efx purposes
    public void guiEFX(){
        try{ 
            Thread.sleep(750);
        } catch (InterruptedException e) {

        }
    }
}