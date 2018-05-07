package com.generic.base;

import java.util.AbstractSequentialList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ListIterator;
import java.util.NoSuchElementException;

// ==================================================================
// "skip list" data structure
//
// This class provides "direct" access to its skip-list nodes
// through methods like "getNode(int index)"..
// ==================================================================

public final class SkipList<E> extends AbstractSequentialList<E> {

  private int size;
  private Node<E> head;
  private Node<E> tail;

  // ==================================================================
  // list node element
  // ==================================================================

  public static class Node<E> {
    private E item = null;
    private Node<E> prev = null;
    private Node<E> next = null;
    private Skip<E>[] nextSkips = null;
    private Skip<E>[] prevSkips = null;

    public Node() {
      item = null;
      prev = null;
      next = null;
    }
    public E getItem() {
      return item;
    }
    public Node<E> getPrev() {
      return prev;
    }
    public Node<E> getNext() {
      return next;
    }
    public int getIndex() {
      return SkipList.distanceFromLeftEnd(this);
    }
    public Node<E> getNextOrPrev(boolean prevNotNext) {
      return prevNotNext ? prev : next;
    }

    // ---------------------------------------------------------
    protected void setItem(E item) {
      this.item = item;
    }

    // ---------------------------------------------------------
    @SuppressWarnings("unchecked")
    private void newSkipArrays(int height) {
      if (height > 0) {
        nextSkips = (Skip<E>[]) new Skip<?>[height];
        prevSkips = (Skip<E>[]) new Skip<?>[height];
      } else {
        nextSkips = null;
        prevSkips = null;
      }
    }
    private int getSkipHeight() {
      return (nextSkips == null) ? 0 : nextSkips.length;
    }
    private Skip<E>[] getNextOrPrevSkips(boolean prevNotNext) {
      return prevNotNext ? prevSkips : nextSkips;
    }
  }

  // ==================================================================
  // "skip" objects
  // ==================================================================

  private static final class Skip<E> {
    private final Node<E> prev;
    private final Node<E> next;
    private final int offset;

    private Skip(Node<E> prev, Node<E> next, int offset) {
      this.prev = prev;
      this.next = next;
      this.offset = offset;
    }
    private Node<E> getNextOrPrev(boolean prevNotNext) {
      return prevNotNext ? prev : next;
    }
  }

  // ==================================================================
  // "index/node" pairs
  // ==================================================================

  private static final class IndexNodePair<E> {
     public int index;
     public Node<E> node;
     
     private static <E> IndexNodePair<E> of(int index, Node<E> node) {
        IndexNodePair<E> result = new IndexNodePair<E>();
        result.index = index;
        result.node = node;
        return result;
     }
  }
  
  // ==================================================================
  // constructors
  // ==================================================================

  public SkipList() {
    this(0, null, null);
  }
  private SkipList(int size, Node<E> head, Node<E> tail) {
    this.size = size;
    this.head = head;
    this.tail = tail;
  }
  public SkipList(Collection<? extends E> items) {
    Builder<E> builder = new Builder<E>();
    for (E item : items) {
      builder.add(item);
    }

    SkipList<E> list = builder.build();
    this.size = list.size;
    this.head = list.head;
    this.tail = list.tail;
  }

  private SkipList<E> newSkipList(int size, Node<E> head, Node<E> tail) {
    return new SkipList<E>(size, head, tail);
  }
  private SkipList<E> newSkipList(Collection<? extends E> items) {
    return new SkipList<E>(items);
  }

  public static class Builder<E> {
    private SkipList<E> list = new SkipList<E>();
    private ArrayList<IndexNodePair<E>> rightmostHighNodes = new ArrayList<IndexNodePair<E>>();

    public Node<E> add(E item) {
      Node<E> element = SkipList.<E>newNodeForItem(item);

      // add each new element to the tail..
      if (list.tail == null) {
        list.head = element;
        list.tail = element;
      } else {
        list.tail.next = element;
        element.prev = list.tail;
        list.tail = element;
      }

      // link and update rightmost high elements
      linkNextNode(element, list.size, rightmostHighNodes);
      list.size++;
      return element;
    }

    public SkipList<E> build() {
      return list;
    }
  }

  // ==================================================================
  // basic list operations
  // ==================================================================

  @Override
  public int size() {
    return size;
  }
  @Override
  public void clear() {
    size = 0;
    head = null;
    tail = null;
  }
  @Override
  public E get(int index) {
    RangeChecking.checkNodeIndex(index, size);
    return getNode(index).item;
  }
  @Override
  public E set(int index, E newItem) {
    RangeChecking.checkNodeIndex(index, size);
    Node<E> element = getNode(index);
    E oldItem = element.item;
    element.item = newItem;
    return oldItem;
  }
  @Override
  public E remove(int index) {
    RangeChecking.checkNodeIndex(index, size);
    Node<E> element = getNode(index);
    E oldItem = element.item;
    removeNode(element);
    return oldItem;
  }
  @Override
  public boolean addAll(Collection<? extends E> items) {
    return addAll(size, items);
  }
  @Override
  public void add(int index, E item) {
    RangeChecking.checkPositionIndex(index, size);
    Node<E> rightOfInsertionPoint = (index == size) ? null : getNode(index);
    insertItemBefore(item, rightOfInsertionPoint);
  }
  @Override
  public boolean addAll(int index, Collection<? extends E> items) {
    RangeChecking.checkPositionIndex(index, size);
    if (items.isEmpty()) {
      return false;
    }
    destructivelyInsertSublist(newSkipList(items), index);
    return true;
  }

  // ==================================================================
  // list iterator
  // ==================================================================

  @Override
  public ListIterator<E> listIterator(int index) {
    RangeChecking.checkPositionIndex(index, size);
    return new Itr(index);
  }

  private class Itr implements ListIterator<E> {
    private Node<E> lastReturned = null;
    private Node<E> next = null;
    private int nextIndex;

    private Itr(int index) {
      this.next = (index == size) ? null : getNode(index);
      this.nextIndex = index;
      this.lastReturned = null;
    }

    @Override
    public boolean hasNext() {
      return nextIndex < size;
    }
    @Override
    public boolean hasPrevious() {
      return nextIndex > size;
    }
    @Override
    public int nextIndex() {
      return nextIndex;
    }
    @Override
    public int previousIndex() {
      return nextIndex - 1;
    }
    @Override
    public E next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      lastReturned = next;
      next = next.next;
      nextIndex++;
      return lastReturned.item;
    }
    @Override
    public E previous() {
      if (!hasPrevious()) {
        throw new NoSuchElementException();
      }
      next = (next == null) ? tail : next.prev;
      lastReturned = next;
      nextIndex--;
      return lastReturned.item;
    }
    @Override
    public void set(E item) {
      if (lastReturned == null) {
        throw new IllegalStateException();
      }
      lastReturned.item = item;
    }
    @Override
    public void remove() {
      if (lastReturned == null) {
        throw new IllegalStateException();
      }

      Node<E> lastNext = lastReturned.next;
      removeNode(lastReturned);
      if (next == lastReturned) {
        next = lastNext;
      } else {
        nextIndex--;
      }
      lastReturned = null;
    }
    @Override
    public void add(E item) {
      lastReturned = null;
      insertItemBefore(item, next);
      nextIndex++;
    }
  }

  // ==================================================================
  // range checking
  // ==================================================================

  public static class RangeChecking {

    /**
     * Tells if the argument is the index of an existing element.
     */
    private static boolean isNodeIndex(int index, int size) {
      return index >= 0 && index < size;
    }
    public static void checkNodeIndex(int index, int size) {
      if (!isNodeIndex(index, size)) {
        throw newOutOfBoundsException(index, size);
      }
    }

    /**
     * Tells if the argument is the index of a valid position for an
     * iterator or an add operation.
     */
    private static boolean isPositionIndex(int index, int size) {
      return index >= 0 && index <= size;
    }
    public static void checkPositionIndex(int index, int size) {
      if (!isPositionIndex(index, size)) {
        throw newOutOfBoundsException(index, size);
      }
    }

    /**
     * Constructs an IndexOutOfBoundsException
     */
    private static IndexOutOfBoundsException newOutOfBoundsException(int index, int size) {
      return new IndexOutOfBoundsException("{ Index = " + index + ", size = " + size + " }");
    }
  }

  // ==================================================================
  // navigation
  // ==================================================================

  public Node<E> getNode(int index) {
    return (index < (size >> 1)) ? moveRight(head, index) : moveLeft(tail, size - 1 - index);
  }

  // ==================================================================
  // removal
  // ==================================================================

  @Override
  protected void removeRange(int fromIndex, int toIndex) {
    removeSublist(fromIndex, toIndex);
  }
  public void removeNode(Node<E> element) {
    removeSublist(element, element, 1);
  }
  public SkipList<E> removeSublist(int fromIndex, int toIndex) {
    int rangeSize = toIndex - fromIndex;

    Node<E> first = getNode(fromIndex);
    Node<E> last = (rangeSize < ((size - fromIndex) >> 1)) ?
        moveRight(first, rangeSize - 1) : moveLeft(tail, size - toIndex);

    return removeSublist(first, last, rangeSize);
  }

  private SkipList<E> removeSublist(Node<E> first, Node<E> last, int rangeSize) {
    Node<E> elementLeftOfFirst = first.prev;
    Node<E> elementRightOfLast = last.next;

    // FIRST find the high elements before affecting the list:
    ArrayList<IndexNodePair<E>> highNodesLeftOfFirst
      = highNodesLeft(elementLeftOfFirst);
    ArrayList<IndexNodePair<E>> highNodesRightOfLast
      = highNodesRight(elementRightOfLast);
    int minHeight = Math.min(highNodesLeftOfFirst.size(), highNodesRightOfLast.size());

    // SECOND a "normal" doubly-linked-list removal:
    if (elementLeftOfFirst == null) {
      // deleting from the beginning of the list...

      if (elementRightOfLast == null) {
        // deleting the entire list.  allrighty then:
        head = null;
        tail = null;

      } else {
        // deleting the left portion of the list,
        // so "elementRightOfLast" becomes the new head
        head = elementRightOfLast;
        elementRightOfLast.prev = null;
      }
    } else {
      if (elementRightOfLast == null) {
        // deleting the right portion of the list,
        // so "elementLeftOfFirst" becomes the new tail
        tail = elementLeftOfFirst;
        elementLeftOfFirst.next = null;

      } else {
        // we're deleting a middle portion of the list
        elementLeftOfFirst.next = elementRightOfLast;
        elementRightOfLast.prev = elementLeftOfFirst;
      }
    }
    first.prev = null;
    last.prev = null;

    // THIRD we fix up the 'skips':
    // high elements on either side need to be connected with skips:
    for (int height = 0; height < minHeight; height++) {
      IndexNodePair<E> leftOfFirst = highNodesLeftOfFirst.get(height);
      IndexNodePair<E> rightOfLast = highNodesRightOfLast.get(height);

      Skip<E> skipOnLeft  = leftOfFirst.node.nextSkips[height];
      Skip<E> skipOnRight = rightOfLast.node.prevSkips[height];
      if (skipOnLeft != skipOnRight) {
        // Detach outward-pointing skips from the deleted region
        skipOnLeft.next.prevSkips[height] = null;
        skipOnRight.prev.nextSkips[height] = null;
      }

      // Create a new skip:
      Skip<E> skip = new Skip<E>(leftOfFirst.node, rightOfLast.node,
        leftOfFirst.index + 1 + rightOfLast.index);

      // Link them:
      leftOfFirst.node.nextSkips[height] = skip;
      rightOfLast.node.prevSkips[height] = skip;
    }
    // unmatched high elements on either side are cleared:
    for (int height = minHeight; height < highNodesRightOfLast.size(); height++) {
      Node<E> highNodeAtHeight = highNodesRightOfLast.get(height).node;
      highNodeAtHeight.prevSkips[height] = null;
    }
    for (int height = minHeight; height < highNodesLeftOfFirst.size(); height++) {
      Node<E> highNodeAtHeight = highNodesLeftOfFirst.get(height).node;
      highNodeAtHeight.nextSkips[height] = null;
    }

    // FINALLY we reduce size by the number of deleted elements,
    // which must be provided:
    size -= rangeSize;

    // Return the deleted region which is now detached and self-contained:
    return newSkipList(rangeSize, first, last);
  }

  // ==================================================================
  // insertion
  // ==================================================================

  public void insertItemBefore(E item, Node<E> rightOfInsertionPoint) {
    Node<E> newNode = newNodeForItem(item);
    SkipList<E> listToInsert = newSkipList(1, newNode, newNode);
    destructivelyInsertSublist(listToInsert, rightOfInsertionPoint);
  }
  public void destructivelyInsertSublist(SkipList<E> listToInsert,
                                         int indexRightOfInsertionPoint) {
    destructivelyInsertSublist(listToInsert,
        (indexRightOfInsertionPoint == size) ? null : getNode(indexRightOfInsertionPoint));
  }

  private void destructivelyInsertSublist(SkipList<E> listToInsert,
                                          Node<E> rightOfInsertionPoint) {
    Node<E> leftOfInsertionPoint =
        (rightOfInsertionPoint == null) ? tail : rightOfInsertionPoint.prev;

    // FIRST find the high elements before affecting the list:
    ArrayList<IndexNodePair<E>> highNodesLeftOfFirst
      = highNodesLeft(leftOfInsertionPoint);
    ArrayList<IndexNodePair<E>> highNodesRightOfLast
      = highNodesRight(rightOfInsertionPoint);

    ArrayList<IndexNodePair<E>> highNodesLeftOfLast
      = highNodesLeft(listToInsert.tail);
    ArrayList<IndexNodePair<E>> highNodesRightOfFirst
      = highNodesRight(listToInsert.head);

    // Note that "highNodesLeftOfLast" and "highNodesRightOfFirst" will be
    // equal in height, so there are at most 3 unique heights here:

    int maxHeightLeftOfInsertion = highNodesLeftOfFirst.size();
    int maxHeightOfInsertion = highNodesLeftOfLast.size();
    int maxHeightRightOfInsertion = highNodesRightOfLast.size();

    // SECOND a "normal" doubly-linked-list insertion:
    listToInsert.tail.next = rightOfInsertionPoint;
    listToInsert.head.prev = leftOfInsertionPoint;
    if (leftOfInsertionPoint == null) {
       // inserting at the beginning of the list...

       if (rightOfInsertionPoint == null) {
          // inserting the entire list.  allrighty then:
          head = listToInsert.head;
          tail = listToInsert.tail;

       } else {
          // inserting a range at head...
          head = listToInsert.head;
          rightOfInsertionPoint.prev = listToInsert.tail;

       }
    } else {
       if (rightOfInsertionPoint == null) {
          // inserting a range at tail...
          leftOfInsertionPoint.next = listToInsert.head;
          tail = listToInsert.tail;

       } else {
          // inserting into the middle portion of the list
          leftOfInsertionPoint.next = listToInsert.head;
          rightOfInsertionPoint.prev = listToInsert.tail;
       }
    }

    // THIRD we fix up the 'skips':
    // weld matching skips on the left side...
    int minHeightFirst = Math.min(maxHeightLeftOfInsertion,  maxHeightOfInsertion);
    for (int height = 0; height < minHeightFirst; height++) {
       IndexNodePair<E> leftOfFirst = highNodesLeftOfFirst.get(height);
       IndexNodePair<E> rightOfFirst = highNodesRightOfFirst.get(height);

       // Create a new skip:
       Skip<E> skip = new Skip<E>(leftOfFirst.node, rightOfFirst.node,
          leftOfFirst.index + 1 + rightOfFirst.index);

       // Link them:
       skip.prev.nextSkips[height] = skip;
       skip.next.prevSkips[height] = skip;
    }

    // weld matching skips on the right side...
    int minHeightLast  = Math.min(maxHeightRightOfInsertion, maxHeightOfInsertion);
    for (int height = 0; height < minHeightLast; height++) {
       IndexNodePair<E> leftOfLast = highNodesLeftOfLast.get(height);
       IndexNodePair<E> rightOfLast = highNodesRightOfLast.get(height);

       // Create a new skip:
       Skip<E> skip = new Skip<E>(leftOfLast.node, rightOfLast.node,
          leftOfLast.index + 1 + rightOfLast.index);

       // Link them:
       skip.prev.nextSkips[height] = skip;
       skip.next.prevSkips[height] = skip;
    }

    // weld skips which jump over the entire inserted range
    int minHeightOuter = Math.min(maxHeightLeftOfInsertion, maxHeightRightOfInsertion);
    for (int height = maxHeightOfInsertion; height < minHeightOuter; height++) {
      IndexNodePair<E> leftOfFirst = highNodesLeftOfFirst.get(height);
      IndexNodePair<E> rightOfLast = highNodesRightOfLast.get(height);

      // Create a new skip:
      Skip<E> skip = new Skip<E>(leftOfFirst.node, rightOfLast.node,
          leftOfFirst.index + listToInsert.size + rightOfLast.index);

      // Link them:
      skip.prev.nextSkips[height] = skip;
      skip.next.prevSkips[height] = skip;
    }

    // FINALLY we increase size by the number of inserted elements,
    // which must be provided:
    size += listToInsert.size;
    listToInsert.clear();
  }

  // ==================================================================
  // find the first elements of each height (from a given start)
  // ==================================================================

  private static <E> ArrayList<IndexNodePair<E>> highNodesRight(Node<E> start) {
    return highNodes(start, false);
  }
  private static <E> ArrayList<IndexNodePair<E>> highNodesLeft(Node<E> start) {
    return highNodes(start, true);
  }
  private static <E> ArrayList<IndexNodePair<E>> highNodes(Node<E> start, boolean prevNotNext) {

    ArrayList<IndexNodePair<E>> elements = new ArrayList<IndexNodePair<E>>();
    Node<E> currentNode = start;
    if (currentNode == null) {
      // there's nothing to left of start at all
      return elements;
    }

    int amountMoved = 0;
    int currentHeight = 0;
    while (true) {

      // if we're at a height step-up, add this element to the result array:
      int currentNodeHeight = currentNode.getSkipHeight();
      while (currentHeight < currentNodeHeight) {
        elements.add(IndexNodePair.of(amountMoved, currentNode));
        currentHeight++;
      }

      if (currentHeight == 0) {
        // If we're still on ground-level, just move one element to the left
        amountMoved++;
        currentNode = currentNode.getNextOrPrev(prevNotNext);
        if (currentNode == null) {
          // we've reached the left side of the list still at ground-level
          return elements;
        }

      } else {
        // get the highest skip
        Skip<E> skip = (currentNode.getNextOrPrevSkips(prevNotNext))[currentHeight - 1];
        if (skip == null) {
          // we've reached the end of the skip, we're done.
          return elements;
        }
        amountMoved += skip.offset;
        currentNode = skip.getNextOrPrev(prevNotNext);
      }
    }
  }

  // ==================================================================
  // count the number of elements until the end (from a given start)
  // ==================================================================

  public static <E> int distanceFromRightEnd(Node<E> start) {
    return distanceFromEnd(start, false);
  }
  public static <E> int distanceFromLeftEnd(Node<E> start) {
    return distanceFromEnd(start, true);
  }
  private static <E> int distanceFromEnd(Node<E> start, boolean prevNotNext) {
    Node<E> currentNode = start;
    int amountMoved = 0;
    while (true) {
      int currentNodeHeight = currentNode.getSkipHeight();

      // use the highest non-null skip
      for (int height = currentNodeHeight - 1; height >= 0; --height) {
        Skip<E> skip = (currentNode.getNextOrPrevSkips(prevNotNext))[height];
        if (skip != null) {
          amountMoved += skip.offset;
          currentNode = skip.getNextOrPrev(prevNotNext);
          continue;
        }
      }

      // no skips are available...
      currentNode = currentNode.getNextOrPrev(prevNotNext);
      if (currentNode == null) {
        return amountMoved;
      }
      amountMoved++;
    }
  }

  // ==================================================================
  // move a specific number of elements (from a given start)
  // ==================================================================

  public static <E> Node<E> moveRight(Node<E> start, int offset) {
    return move(start, offset, false);
  }
  public static <E> Node<E> moveLeft(Node<E> start, int offset) {
    return move(start, offset, true);
  }
  private static <E> Node<E> move(Node<E> start, int offset, boolean prevNotNext) {
    Node<E> currentNode = start;
    int amountMoved = 0;
    if (offset == 0) {
      return currentNode;
    }

    // first move while increasing height if possible
    boolean descending = false;
    int currentHeight = 0;
    while (true) {
      int offsetToNextNodeAtCurrentHeight = 0;
      Node<E> nextNodeAtCurrentHeight = null;

      if (currentHeight == 0) {
        offsetToNextNodeAtCurrentHeight = 1;
        nextNodeAtCurrentHeight = currentNode.getNextOrPrev(prevNotNext);

      } else {
        Skip<E> skip = (currentNode.getNextOrPrevSkips(prevNotNext))[currentHeight - 1];
        if ((skip == null) || (amountMoved + skip.offset > offset)) {
          // there are no more skips at this level,
          // or else the skip at this level takes us beyond destination,
          // so we must descend
          descending = true;

        } else {
          offsetToNextNodeAtCurrentHeight = skip.offset;
          nextNodeAtCurrentHeight = skip.getNextOrPrev(prevNotNext);
        }
      }
      if (nextNodeAtCurrentHeight == null) {
        if (currentHeight == 0) {
          // we've run out of elements
          return null;
        }
        // go down without moving elements...
        currentHeight--;
        continue;

      } else {
        if (amountMoved + offsetToNextNodeAtCurrentHeight == offset) {
          // the skip at this level points straight at the target, we're done
          return nextNodeAtCurrentHeight;
        }

        if (!descending) {
          // ok, we could skip, but is a bigger, larger skip available?
          int currentNodeHeight = currentNode.getSkipHeight();
          if (currentNodeHeight > currentHeight) {
            // go up without moving elements...
            currentHeight++;
            continue;
          }
        }
        amountMoved += offsetToNextNodeAtCurrentHeight;
        currentNode = nextNodeAtCurrentHeight;
      }
    }
  }

  // ==================================================================
  // private!
  // ==================================================================

  private static <E> void linkNextNode(Node<E> element, int index,
        ArrayList<IndexNodePair<E>> highNodes) {

    int elementHeight = element.getSkipHeight();
    while (highNodes.size() < elementHeight) {
      highNodes.add(IndexNodePair.of(0, null));
    }
    for (int height = 0; height < elementHeight; ++height) {
      IndexNodePair<E>last = highNodes.get(height);
      Node<E> lastNodeAtHeight = last.node;
      if (lastNodeAtHeight != null) {
        int indexOfLastNode = last.index;
        Skip<E> skip = new Skip<E>(lastNodeAtHeight, element,
           index - indexOfLastNode);

        lastNodeAtHeight.nextSkips[height] = skip;
        element.prevSkips[height] = skip;
      }
      highNodes.set(height, IndexNodePair.of(index, element));
    }
  }

  // ==================================================================
  // randomized height selection
  // ==================================================================

  private static final double FREQUENCY = 3;
  private static final int MAX_HEIGHT = 20;

  private static int randomNodeHeight() {
    int height = -1;
    double v = Math.random();
    while ((v <= 1.0) && (height < MAX_HEIGHT)) {
      height++;
      v *= FREQUENCY;
    }
    return height;
  }

  private static <E> Node<E> newNodeForItem(E item) {
    Node<E> element = new Node<E>();
    element.setItem(item);
    element.newSkipArrays(randomNodeHeight());
    return element;
  }
}
