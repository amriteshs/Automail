# Automail
An automated mail sorting and delivery system, utilizing delivery robots  

The system consists of two key components:
• **Delivery Robots** which take mail items from the mail room and deliver them throughout the building. Each robot can hold one item in its "hands" and one item in its "tube" (a backpack-like container attached to each robot). If a robot is holding two items (i.e., one in its hands and one it its tube) it will always deliver the item in its hands first. An installation of Automail can manage a team of delivery robots of any reasonable size (including zero!).
• A **Mail Pool subsystem** which holds mail items after their arrival at the building's mail room. The mail pool decides the order in which mail items should be delivered.

### Robot caution mode functionality  
• The robot can carry fragile items only with the special arms (not in the tube or with its other arms).  
• The special arms can handle the tasks of wrapping the fragile item with a protective shield (which takes two units of time) before moving and unwrapping the item (which takes one unit of time) before delivery.  
• A robot delivering a fragile item must have sole access to the delivery floor for that item while it unwraps and delivers the item. That is, it can’t deliver that item on a floor with another robot present, and another robot can’t enter that floor while the robot with the fragile item is delivering it.  

### Statistics recorded  
• The number of packages delivered normally (i.e., not using caution).  
• The number of packages delivered using caution.  
• The total weight of the packages delivered normally (i.e., not using caution).  
• The total weight of the packages delivered using caution.  
• The total amount of time spent by the special arms wrapping & unwrapping items.  

### Other useful information:  
• The mailroom is on the ground floor (floor 0).  
• All mail items are stamped with their time of arrival.  
• Fragile mail items arrive at the mailpool and are registered one at a time, so timestamps are unique for fragile items. Normal (non-fragile) mail items arrive at the mailpool in batches, so all items in a normal batch receive the same timestamp.  
• If a mail item is ‘fragile’ and ‘caution’ mode is turned on, the system must handle the item as described above (wrap, deliver in isolation, unwrap).  
• The mailpool is responsible for giving mail items to the robots for delivery.  
• A Delivery Robot carries at most three items including one fragile item on the special arms, and it can be sent to deliver mail if it has at least one item on either normal or special arms.  
• All mail items have a weight. Delivery robots can carry items of any weight, and place items of any weight in their tube if the items are not fragile.  
• The system generates a measure of the effectiveness of the system in delivering all mail items, taking into account time to deliver and types of mail items (normal, non-fragile).  
