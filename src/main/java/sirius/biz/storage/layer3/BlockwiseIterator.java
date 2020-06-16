/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.storage.layer3;

import sirius.kernel.commons.Limit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

class BlockwiseIterator implements Iterator<VirtualFile> {

    private static final int BLOCK_SIZE = 1000;

    private VirtualFile virtualFile;
    private int nextStart = 0;
    private Iterator<VirtualFile> currentBlock;

    protected BlockwiseIterator(VirtualFile virtualFile) {
        this.virtualFile = virtualFile;
        this.currentBlock = fetchNextBlock();
    }

    private Iterator<VirtualFile> fetchNextBlock() {
        List<VirtualFile> buffer = new ArrayList<>();
        virtualFile.children(new FileSearch(buffer::add).withLimit(new Limit(nextStart, BLOCK_SIZE)));
        if (buffer.isEmpty()) {
            return null;
        }

        nextStart += buffer.size();
        return buffer.iterator();
    }

    @Override
    public boolean hasNext() {
        return currentBlock != null;
    }

    @Override
    public VirtualFile next() {
        if (currentBlock != null && !currentBlock.hasNext()) {
            currentBlock = fetchNextBlock();
        }

        if (currentBlock == null) {
            throw new NoSuchElementException();
        }

        return currentBlock.next();
    }
}
