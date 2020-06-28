package strategies;

import java.util.LinkedList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.ListIterator;

import automail.Clock;
import automail.MailItem;
import automail.Robot;
import automail.Robot.RobotState;
import exceptions.BreakingFragileItemException;
import exceptions.ItemTooHeavyException;
import exceptions.NormalItemException;
import javafx.util.Pair;

public class MailPool implements IMailPool {
	
	private class Item {
		int destination;
		MailItem mailItem;
		
		public Item(MailItem mailItem) {
			destination = mailItem.getDestFloor();
			this.mailItem = mailItem;
		}
	}
	
	public class ItemComparator implements Comparator<Item> {
		@Override
		public int compare(Item i1, Item i2) {
			int order = 0;
			if (i1.destination > i2.destination) {  // Further before closer
				order = 1;
			} else if (i1.destination < i2.destination) {
				order = -1;
			}
			return order;
		}
	}
	
	private LinkedList<Item> pool;
	private LinkedList<Robot> robots;

	public MailPool(int nrobots){
		// Start empty
		pool = new LinkedList<Item>();
		robots = new LinkedList<Robot>();
	}

	public void addToPool(MailItem mailItem) {
		Item item = new Item(mailItem);
		pool.add(item);
		pool.sort(new ItemComparator());
	}
	
	@Override
	public void step(boolean cautionEnabled) throws ItemTooHeavyException, BreakingFragileItemException, NormalItemException {
		try{
			ListIterator<Robot> i = robots.listIterator();
			while (i.hasNext()) loadRobot(i, cautionEnabled);
		} catch (Exception e) { 
            throw e; 
        } 
	}
	
	private void loadRobot(ListIterator<Robot> i, boolean cautionEnabled) throws ItemTooHeavyException, BreakingFragileItemException, NormalItemException {
		Robot robot = i.next();
		assert(robot.isEmpty());
		// System.out.printf("P: %3d%n", pool.size());
		ListIterator<Item> j = pool.listIterator();
		
		boolean flagNormalHand = false; // flag to check if robot has normal item
		boolean flagTube = false;		// flag to check if robot has item in tube
		boolean flagSpecialHand = false;// flag to check if robot has fragile item
		
		if (pool.size() > 0) {
			try {
				/*
				 *  loop iterates over the items in the pool
				 *  breaks when either iteration is completed or robot has 3 items (2 normal, 1 fragile)
				 */
				while (true) {
					Item k = j.next();
					boolean isFragile = k.mailItem.getFragile();	// check if item is fragile
					
					if (isFragile) {
						// if item is fragile but caution=FALSE (disabled), throw exception
						if (!cautionEnabled) throw new BreakingFragileItemException();
						
						// if current item is fragile and robot is not carrying fragile item, add it to robot
						if (!flagSpecialHand) {
							robot.addToSpecialHand(k.mailItem);	// load item onto robot's special arms
							j.remove();							// remove item from mailpool queue
							flagSpecialHand = true;				// indicates that robot is now carrying fragile item
						}
					} else {
						// if current item is normal
						// if robot's hand is empty, load item onto hand, else load it into tube
						if (!flagNormalHand) {
							robot.addToHand(k.mailItem);	// load item onto robot's hand
							j.remove();						// remove item from mailpool queue
							flagNormalHand = true;			// indicates that robot is now carrying normal item in hand
						} else if (!flagTube) {
							robot.addToTube(k.mailItem);	// load item onto robot's hand
							j.remove();						// remove item from mailpool queue
							flagTube = true;				// indicates that robot is now carrying normal item in tube
						}
					}
					
					// break loop if robot has 3 items or iteration over pool completes
					if (!j.hasNext() || pool.size() == 0 || (flagNormalHand && flagSpecialHand && flagTube)) {
						break;
					}
				}

				robot.dispatch(); // send the robot off if it has any items to deliver
				i.remove();       // remove from mailPool queue
			} catch (Exception e) { 
	            throw e; 
	        } 
		}
	}

	@Override
	public void registerWaiting(Robot robot) { // assumes won't be there already
		robots.add(robot);
	}
}
