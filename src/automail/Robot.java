package automail;

import exceptions.BreakingFragileItemException;
import exceptions.ExcessiveDeliveryException;
import exceptions.ItemTooHeavyException;
import exceptions.NormalItemException;
import javafx.util.Pair;
import strategies.IMailPool;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import java.lang.Math;

/**
 * The robot delivers mail!
 */
public class Robot {
	
    static public final int INDIVIDUAL_MAX_WEIGHT = 2000;

    IMailDelivery delivery;
    protected final String id;
    /** Possible states the robot can be in */
    public enum RobotState { DELIVERING, WAITING, RETURNING }
    public RobotState current_state;
    private int current_floor;
    private int destination_floor;
    private IMailPool mailPool;
    private boolean receivedDispatch;
    
    private MailItem deliveryItem = null;
    private MailItem tube = null;
    private MailItem specialItem = null;	// fragile item; "null" signifies robot not carrying fragile item
    
    private int deliveryCounter;

    private MailItem currentlyDelivering = null;	// the item being delivered by robot currently
    private int timeDelayed = -1;					// the time when robot will become active again; timeDelayed != -1 signifies robot is wrapping or unwrapping fragile item 
    private RobotState nextStateDelayed = null;		// the robot's next state; this change will occur at timeDelayed time (when != -1) 
    
    /**
     * Initiates the robot's location at the start to be at the mailroom
     * also set it to be waiting for mail.
     * @param behaviour governs selection of mail items for delivery and behaviour on priority arrivals
     * @param delivery governs the final delivery
     * @param mailPool is the source of mail items
     */
    public Robot(IMailDelivery delivery, IMailPool mailPool){
    	id = "R" + hashCode();
        // current_state = RobotState.WAITING;
    	current_state = RobotState.RETURNING;
        current_floor = Building.MAILROOM_LOCATION;
        this.delivery = delivery;
        this.mailPool = mailPool;
        this.receivedDispatch = false;
        this.deliveryCounter = 0;
    }
    
    public void dispatch() {
    	receivedDispatch = true;
    }
    
    // check if robot is currently delivering fragile item and is on the destination floor
    public boolean isDeliveringFragile() {
    	if (currentlyDelivering == specialItem && current_state == RobotState.DELIVERING && current_floor == destination_floor) {
    		return true;
    	}
    	
    	return false;
    }

    /**
     * This is called on every time step
     * @throws ExcessiveDeliveryException if robot delivers more than the capacity of the tube without refilling
     */
    public void step(boolean flag) throws ExcessiveDeliveryException {
    	if (flag && current_state == RobotState.DELIVERING) {
    		/* 
    		 * if another robot has arrived to deliver fragile item on the same floor,
    		 * current robot will move to another floor
    		 */
    		if (current_floor == destination_floor) {
    			moveTowards(Building.MAILROOM_LOCATION);
    		} else {
    			moveTowards(destination_floor);
    		}
    		
    		return;
    	}
    	
    	// check if robot is currently wrapping or wrapping fragile item
    	if (timeDelayed != -1) {
    		if (timeDelayed <= Clock.Time()) {
    			// if current time exceeds timeDelayed time, robot becomes active again
    			if (nextStateDelayed == RobotState.DELIVERING) {
    				// set new route for robot
    				setRoute();
    			} else if (nextStateDelayed == RobotState.RETURNING) {
    				// robot returning; no items being currently delivered
    				currentlyDelivering = null;
    			}
    			// change robot's state
    			changeState(nextStateDelayed);
    			
    			// reset delay values
    			timeDelayed = -1;
    			nextStateDelayed = null;
    		}
    		
    		// break execution
    		return;
    	}
    	
    	switch(current_state) {
    		/** This state is triggered when the robot is returning to the mailroom after a delivery */
    		case RETURNING:
    			/** If its current position is at the mailroom, then the robot should change state */
                if(current_floor == Building.MAILROOM_LOCATION){
                	if (tube != null) {
                		mailPool.addToPool(tube);
                        System.out.printf("T: %3d >  +addToPool [%s]%n", Clock.Time(), tube.toString());
                        tube = null;
                	}
        			/** Tell the sorter the robot is ready */
        			mailPool.registerWaiting(this);
                	changeState(RobotState.WAITING);
                } else {
                	/** If the robot is not at the mailroom floor yet, then move towards it! */
                    moveTowards(Building.MAILROOM_LOCATION);
                	break;
                }
    		case WAITING:
                /** If the StorageTube is ready and the Robot is waiting in the mailroom then start the delivery */
                if(!isEmpty() && receivedDispatch) {
                	// if robot receives fragile item, update timeWrap statistic
                	if (specialItem != null) {
                		Statistics.timeWrapIncrement();
                	}
                	
                	receivedDispatch = false;
                	deliveryCounter = 0; // reset delivery counter
                	
        			if (specialItem != null) {
        				// if robot has fragile item, delay start of delivery by 2 units of time
        				timeDelayed = Clock.Time() + 2;
    					nextStateDelayed = RobotState.DELIVERING;
        			} else {
        				// if robot has only normal item(s), start delivery without delay
        				setRoute();
        				changeState(RobotState.DELIVERING);
        			}
        		}
                break;
    		case DELIVERING:
    			if(current_floor == destination_floor){ // If already here drop off either way
    				/** Delivery complete, report this to the simulator! */
    				if (currentlyDelivering == deliveryItem) {
    					// if robot currently delivering normal item, drop it off
                    	delivery.deliver(deliveryItem);
                    	
                    	// update packagesNormal and weightNormal statistics
                    	Statistics.packagesNormalIncrement();
                    	Statistics.weightNormalIncrement(deliveryItem.getWeight());
                    	
                    	deliveryItem = null;	// set normal item to "null"
                    	deliveryCounter++;
        				if (deliveryCounter > 3) {  // Implies a simulation bug
                        	throw new ExcessiveDeliveryException();
                        }
        				
        				if (tube == null && specialItem == null) {
        					// if robot has neither fragile item nor item in tube, start returning
        					currentlyDelivering = null;
    						changeState(RobotState.RETURNING);
    					} else {
    						if (tube != null) {
    							// if robot has item in tube, transfer it to hand
    							deliveryItem = tube;
    							tube = null;
    						}
    						
    						// set route for new destination (either for normal or fragile item)
    						setRoute();
    						changeState(RobotState.DELIVERING);
    					}
                    } else {
                    	// if robot currently delivering fragile item, drop it off
                    	delivery.deliver(specialItem);
                    	
                    	// update packagesCaution, weightCaution and timeUnwrap statistics
                    	Statistics.packagesCautionIncrement();
                    	Statistics.weightCautionIncrement(specialItem.getWeight());
                    	Statistics.timeUnwrapIncrement();
                    	
                    	specialItem = null;		// set fragile item to "null"
                    	deliveryCounter++;
        				if (deliveryCounter > 3) {  // Implies a simulation bug
                        	throw new ExcessiveDeliveryException();
                        }
        				
        				if (deliveryItem == null) {
        					// if robot not carrying any items, start returning after delaying by 1 unit of time
        					timeDelayed = Clock.Time() + 1;
        					nextStateDelayed = RobotState.RETURNING;
    					} else {
    						// if robot still carrying normal item(s), start delivery after delaying by 1 unit of time
    						timeDelayed = Clock.Time() + 1;
        					nextStateDelayed = RobotState.DELIVERING;
    					}
                    }
    			} else {
	        		/** The robot is not at the destination yet, move towards it! */
	                moveTowards(destination_floor);
    			}
                break;
    	}
    }

    /**
     * Sets the route for the robot
     */
    private void setRoute() {
        /** Set the destination floor */
    	if (specialItem != null && deliveryItem != null) {
    		// if robot is carrying both normal and fragile items, set route for the item with the closer destination floor
    		if (Math.abs(current_floor - specialItem.getDestFloor()) >= Math.abs(current_floor - deliveryItem.getDestFloor())) {
    			destination_floor = deliveryItem.getDestFloor();
    			currentlyDelivering = deliveryItem;
    		} else {
    			destination_floor = specialItem.getDestFloor();
    			currentlyDelivering = specialItem;
    		}
    	} else if (specialItem != null) {
    		// if robot is only carrying fragile item, set route for the fragile item's destination floor
    		destination_floor = specialItem.getDestFloor();
    		currentlyDelivering = specialItem;
    	} else if (deliveryItem != null) {
    		// if robot is only carrying normal item(s), set route for the normal item's destination floor
    		destination_floor = deliveryItem.getDestFloor();
    		currentlyDelivering = deliveryItem;
    	}
    }

    /**
     * Generic function that moves the robot towards the destination
     * @param destination the floor towards which the robot is moving
     */
    private void moveTowards(int destination) {
        if(current_floor < destination){
            current_floor++;
        } else {
            current_floor--;
        }
    }
    
    private String getIdTube() {
    	return String.format("%s(%1d)", id, (tube == null ? 0 : 1));
    }
    
    /**
     * Prints out the change in state
     * @param nextState the state to which the robot is transitioning
     */
    private void changeState(RobotState nextState){
    	assert(!(deliveryItem == null && tube != null));
    	if (current_state != nextState) {
            System.out.printf("T: %3d > %7s changed from %s to %s%n", Clock.Time(), getIdTube(), current_state, nextState);
    	}
    	current_state = nextState;
    	if(nextState == RobotState.DELIVERING){
    		// print item details currently being delivered (normal or fragile)
    		if (currentlyDelivering == deliveryItem) {
    			System.out.printf("T: %3d > %9s-> [%s]%n", Clock.Time(), getIdTube(), deliveryItem.toString());
    		} else {
    			System.out.printf("T: %3d > %9s-> [%s]%n", Clock.Time(), getIdTube(), specialItem.toString());
    		}
    	}
    }

	public MailItem getTube() {
		return tube;
	}
    
	static private int count = 0;
	static private Map<Integer, Integer> hashMap = new TreeMap<Integer, Integer>();

	@Override
	public int hashCode() {
		Integer hash0 = super	.hashCode();
		Integer hash = hashMap.get(hash0);
		if (hash == null) { hash = count++; hashMap.put(hash0, hash); }
		return hash;
	}

	public boolean isEmpty() {
		return (deliveryItem == null && tube == null && specialItem == null);
	}

	public void addToHand(MailItem mailItem) throws ItemTooHeavyException, BreakingFragileItemException {
		assert(deliveryItem == null);
		if(mailItem.fragile) throw new BreakingFragileItemException();
		deliveryItem = mailItem;
		if (deliveryItem.weight > INDIVIDUAL_MAX_WEIGHT) throw new ItemTooHeavyException();
	}

	public void addToTube(MailItem mailItem) throws ItemTooHeavyException, BreakingFragileItemException {
		assert(tube == null);
		if(mailItem.fragile) throw new BreakingFragileItemException();
		tube = mailItem;
		if (tube.weight > INDIVIDUAL_MAX_WEIGHT) throw new ItemTooHeavyException();
	}

	// load fragile item onto robot's special arms
	public void addToSpecialHand(MailItem mailItem) throws ItemTooHeavyException, NormalItemException {
		assert(specialItem == null);
		if(!mailItem.fragile) throw new NormalItemException();
		specialItem = mailItem;
		if (specialItem.weight > INDIVIDUAL_MAX_WEIGHT) throw new ItemTooHeavyException();
	}
	
	// get current floor of robot
	public int getCurrentFloor() {
		return current_floor;
	}
}
