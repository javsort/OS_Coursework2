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

    // Variables for memory size
    Integer absoluteSize;
    Integer usableSize;

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
        toUse.previousSize += Allocated.size;
        toUse.size = toUse.size - Allocated.size;

        // Update used block
        Allocated.startAddress = toUse.startAddress;
        Allocated.endAddress = Allocated.pointerReturned + size;
        Allocated.pointerReturned = Allocated.startAddress + ALLOC_HEADER;

        // Update free list address
        toUse.startAddress = Allocated.endAddress + 1;

        // Remaining memory sizes
        absoluteSize = absoluteSize - Allocated.totalsize;
        usableSize = usableSize - Allocated.size;

        // Add free list used back to free list
        addBackToFreeList(toUse);

        System.out.print("memory successfully allocated at " + getMeminHex(Allocated.pointerReturned) + ".\r\n\n");

        // Sort virtual memory by pointers
        sortMemory();

        // Returned pointer allocated
        return Allocated.pointerReturned;
    }

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
            if(!list.isEmpty() && list.get(0).getSize() >= size){

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
        System.out.print("Freeing pointer: " + getMeminHex(ptr) + "...");

        // Check if pointer is valid
        if(ptr < 0){
            System.out.println("Invalid pointer. Please try again.");
            return;
        }	

        UsedBlock current = null;
        FreeBlock newBlock = null;

        // Check if pointer is in used list
        for(int i = 0; i < virtualMemory.size(); i++){
            // Check if its valid if called again or not in list, trigger exception
            try {
                // If its a used block, check if pointer matches
                if(virtualMemory.get(i) instanceof UsedBlock){
                    current = (UsedBlock) virtualMemory.get(i);

                    if(current.pointerReturned == ptr){
                        // If pointer matches, remove from used list and start new free
                        usedBlocks.remove(current);
                        virtualMemory.remove(current);

                        // Erase whatever was stored in the used block
                        current.assignedSp = null;
                        current.size = current.originalSize;

                        // newBlock with previous block characteristics to be updated
                        newBlock = new FreeBlock(current.previousFree, current.previousSize, true, current.size - ALLOC_HEADER, null, null);
                        
                        // update addresses for newBlock
                        newBlock.startAddress = current.startAddress;
                        newBlock.endAddress = current.endAddress;

                        // Put back in free list of appropiate size
                        addBackToFreeList(newBlock);
                        break;
                    } 
                } else if (i == virtualMemory.size() - 1){
                    throw(new Exception ("Pointer not found. Please try again."));

                } else {
                    continue;
                }

            } catch (Exception e) {
                System.out.println("Pointer not found / not valid. Please try again.");
                return;
            }
        }

        System.out.println("\n");
        printCurrentMemory();
        System.out.println("\n");
        
        updateFreeList(newBlock);

        // Sort free blocks
        System.out.print(" memory successfully freed.\r\n\n");
    }

    public void updateFreeList(FreeBlock newFree){
        sortLists();
        sortMemory();

        boolean coalesceToRight = false;
        boolean coalesceToLeft = false;

        int distanceofUsed = 0;

        FreeBlock previousChosen = null;

        for(MemoryBlock current : virtualMemory){
            // if its a used block, add on distance
            if(current instanceof MemoryBlock){
                if(current.previousFree && current.previousSize == newFree.size){
                    distanceofUsed = current.endAddress - newFree.startAddress;
                } else if (!current.previousFree) {
                    distanceofUsed += current.totalsize;
                }
            }

            if(current instanceof FreeBlock){
                // Check if newFree is before
                if(current.startAddress - distanceofUsed == newFree.endAddress){
                    FreeBlock next = (FreeBlock) current;
                    next.setPrevious(newFree);
                    newFree.setNext(next);

                    System.out.println("We found a block before");


                    // Check if the new block is exactly on the left of the newly added next
                    if(newFree.endAddress + 1 == newFree.next.startAddress){
                        // We're coalescing newBlock to the right later
                        coalesceToRight = true;
                    }
                    
                }

                // Check if newFree is after
                if(current.equals(newFree) && previousChosen != null && newFree.startAddress - distanceofUsed == previousChosen.endAddress){
                    FreeBlock prev = (FreeBlock) current;
                    prev.setNext(newFree);
                    newFree.setPrevious(prev);

                    System.out.println("We found a block after");

                    // Check if the new block is exactly on the right of the newly added prev
                    if(newFree.startAddress - 1 == newFree.prev.endAddress){
                        // We're coalescing newBlock to the left later
                        coalesceToLeft = true;
                    }
                }

                previousChosen = (FreeBlock) current;
                System.out.println("Distance of used: " + distanceofUsed);
                System.out.println("Previous Chosen Start Addr: " + getMeminHex(previousChosen.startAddress));
            }
        }

        // Throw these somewhere
        absoluteSize += newFree.totalsize;
        usableSize += newFree.totalsize - FREE_HEADER;

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

        System.out.println("After Coalescing\n");
        printCurrentMemory();
        System.out.println("\n");

    }

    public void coalesce(FreeBlock sent,  int coalesce){
        FreeBlock toConnect = sent;
        removeFromFreeLists(toConnect);

        FreeBlock onLeft;
        FreeBlock onRight;

        FreeBlock joinedBlock;

        switch(coalesce){
            // TO THE LEFT - | OLD FREE BLOCK | NEW FREE BLOCK |
            case 1:
                onLeft = toConnect.prev;

                // Remove from active memory
                removeFromFreeLists(onLeft);
                virtualMemory.remove(toConnect);
                virtualMemory.remove(onLeft);

                joinedBlock = new FreeBlock(onLeft.previousFree, onLeft.previousSize, true, toConnect.endAddress - onLeft.startAddress, onLeft.prev, toConnect.next);

                // Update addresses
                joinedBlock.startAddress = onLeft.startAddress;
                joinedBlock.endAddress = toConnect.endAddress;

                // Update pointers
                addBackToFreeList(joinedBlock);

                System.out.println("Coalesced to the left.");

                break;

            // TO THE RIGHT - | NEW FREE BLOCK | OLD FREE BLOCK |
            case 2:
                onRight = toConnect.next;

                // Remove from active memory
                removeFromFreeLists(onRight);
                virtualMemory.remove(toConnect);
                virtualMemory.remove(onRight);

                joinedBlock = new FreeBlock(toConnect.previousFree, toConnect.previousSize, true, onRight.endAddress - toConnect.startAddress, toConnect.prev, onRight.next);

                // Update addresses
                joinedBlock.startAddress = toConnect.startAddress;
                joinedBlock.endAddress = onRight.endAddress;

                // Update pointers
                addBackToFreeList(joinedBlock);

                System.out.println("Coalesced to the right.");

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

                joinedBlock = new FreeBlock(onLeft.previousFree, onLeft.previousSize, true, onRight.endAddress - onLeft.startAddress, onLeft.prev, onRight.next);

                // Update addresses
                joinedBlock.startAddress = onLeft.startAddress;
                joinedBlock.endAddress = onRight.endAddress;

                // Update pointers
                addBackToFreeList(joinedBlock);

                System.out.println("Coalesced to the left and right.");

                break;

            default:
                System.out.println("How did u get here????");

                break;
        }
    }

    public void removeFromFreeLists(FreeBlock toRemove){
        for(List<MemoryBlock> list : freeLists){
            if(list.contains(toRemove)){
                list.remove(toRemove);
            }
        }
    }

    public void sortMemory(){
        Collections.sort(virtualMemory, new Comparator<MemoryBlock>(){
            @Override
            public int compare(MemoryBlock block1, MemoryBlock block2) {
                return Integer.compare(block1.startAddress, block2.startAddress);
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

        public int getSize(){
            return this.size;
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

            // this.startAddress = previousSize;
            //this.endAddress = startAddress + totalsize;

            usedBlocks.add(this);
            virtualMemory.add(this);
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

        public int getStoredInt(){
            for(int i = assignedSp.length - 1 ; i >= 0; i--){
                if(assignedSp[i] != 0){
                    // Return int in last occupied position in assignedSpace
                    return assignedSp[i];
                }
            }

            return 0;
        }

        public int getPointer(){
            return this.pointerReturned;
        }
    } 

    // run different tests and print results
    public void print(){
        switch (selectedOption) {
            case 1:
                pointer = malloc(28);

                pointer = inputDataString("Testing 1", pointer);

                printCurrentMemory();

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

            case 2:
                pointer = malloc(1024);

                pointer = inputDataInt(728, pointer);
                

                if(pointer < 0){
                    break;
                }
                
                try{
                    System.out.println("Sleeping for 1 second... to simulate cool computing stuff. Think ab ur day for a second :)\n");
                    Thread.sleep(1000);
                } catch (InterruptedException e) {

                }

                printCurrentState(pointer);

                retrieveInt(pointer);

                free(pointer);
                break;

            case 3: 
                int ptr1 = malloc(28);
                int ptr2 = malloc(28);

                printCurrentMemory();
                System.out.println("\n");

                free(ptr1);
                printCurrentMemory();
                System.out.println("\n");

                free(ptr2);
                printCurrentMemory();
                
                

                break;
            default:
                System.out.println("Invalid selection. Unknown Test choice.");
                break;
        }
    }

    public void printCurrentMemory(){
        sortMemory();

        UsedBlock toPrint;
        FreeBlock toPrintFree;

        for(MemoryBlock list : virtualMemory){
            if(list instanceof FreeBlock){
                toPrintFree = (FreeBlock) list;
                System.out.println("Free block of size " + toPrintFree.getSize() + " at " + getMeminHex(toPrintFree.startAddress) + " to " + getMeminHex(toPrintFree.endAddress));
            } else {
                toPrint = (UsedBlock) list;
                System.out.println("Used block of size " + toPrint.getSize() + " at " + getMeminHex(toPrint.startAddress) + " to " + getMeminHex(toPrint.endAddress) + " with pointer " + getMeminHex(toPrint.pointerReturned));
            }
        }
    }

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

    public int inputDataString(String dataString, int pointer){
        System.out.println("Storing the `" + dataString + "` string at: " + getMeminHex(pointer) + "! Please wait...\n");

        for(MemoryBlock checking : virtualMemory){
            if(checking instanceof UsedBlock){
                UsedBlock current = (UsedBlock) checking;
                if(current.pointerReturned == pointer){
                    if(current.inputString(dataString)){
                        return current.pointerReturned;
                    } else {
                        return -1;
                    }
                }
            }
        }

        System.out.println("Pointer not found. Please try again.");
        return -1;
    }

    public void retrieveString(int pointer){
        System.out.println("Retrieving data from pointer: " + getMeminHex(pointer) + ". Please wait...\n");

        for(MemoryBlock checking : virtualMemory){
            if(checking instanceof UsedBlock){
                UsedBlock current = (UsedBlock) checking;
                if(current.pointerReturned == pointer){
                    String storedString = current.getStoredString();
                    System.out.println("String retrieved: '" + storedString + "'\n");
                    return;
                }
            }
        }
        
        System.out.println("Pointer not found. Please try again.");
    }

    public int inputDataInt(int data, int pointer){
        System.out.println("Storing the `" + data + "` int at: " + getMeminHex(pointer) + "! Please wait...\n");

        for(MemoryBlock checking : virtualMemory){
            if(checking instanceof UsedBlock){
                UsedBlock current = (UsedBlock) checking;
                if(current.pointerReturned == pointer){
                    if(current.inputInt(data)){
                        return current.pointerReturned;
                    } else {
                        return -1;
                    }
                }
            }
        }

        System.out.println("Pointer not found. Please try again.");
        return -1;
    }

    public void retrieveInt(int pointer){
        System.out.println("Retrieving data from pointer: " + getMeminHex(pointer) + ". Please wait...\n");

        for(MemoryBlock checking : virtualMemory){
            if(checking instanceof UsedBlock){
                UsedBlock current = (UsedBlock) checking;
                if(current.pointerReturned == pointer){
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

            System.out.println("\r\n==================================");
            System.out.println("          Memory Manager");
            System.out.println("  Select a problem to initialize");
            System.out.println("==================================\r\n");
            System.out.println("1. Test 1 - Request 28 bytes -> store a String -> retrieve String -> free");
            System.out.println("2. Test 2 - Request 28 bytes -> request 1024 bytes -> request 28 bytes -> free 1024 bytes -> request 512 bytes -> free all");
            System.out.println("3. Test 3 - Request 7168 bytes -> request 1024 bytes -> free all");
            System.out.println("4. Test 4 - Request 1024 bytes -> request 28 bytes -> free 28 bytes -> free 28 bytes again");
            System.out.println("5. Test 5");
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

        // Reset pointer
        pointer = 0;

        // Clear used blocks
        usedBlocks.clear();
        virtualMemory.clear();

        // Variables for memory size
        absoluteSize = 8192;
        usableSize = 8192 - FREE_HEADER;

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