/*
 * Copyright 2010 Bruno de Carvalho
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.biasedbit.efflux.packet;

import org.jboss.netty.buffer.ChannelBuffer;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
 */
public class SourceChunkTest {

    @Test
    public void testEncodeDecode() throws Exception {
        long ssrc = 0x0000ffff;
        SdesChunk chunk = new SdesChunk(ssrc);
        chunk.addItem(SdesChunkItems.createCnameItem("cname"));
        chunk.addItem(SdesChunkItems.createNameItem("name"));
        chunk.addItem(SdesChunkItems.createEmailItem("email"));
        chunk.addItem(SdesChunkItems.createPrivItem("prefix", "value"));

        ChannelBuffer encoded = chunk.encode();
        // Must be 32 bit aligned.
        assertEquals(0, encoded.readableBytes() % 4);
        System.err.println("encoded readable bytes: " + encoded.readableBytes());
        SdesChunk decoded = SdesChunk.decode(encoded);

        assertEquals(chunk.getSsrc(), decoded.getSsrc());
        assertNotNull(decoded.getItems());
        assertEquals(4, decoded.getItems().size());

        for (int i = 0; i < chunk.getItems().size(); i++) {
            assertEquals(chunk.getItems().get(i).getType(), decoded.getItems().get(i).getType());
            assertEquals(chunk.getItems().get(i).getValue(), decoded.getItems().get(i).getValue());
        }

        assertEquals(0, encoded.readableBytes());
    }
}
