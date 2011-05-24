/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/
package org.apache.james.mailbox.jpa.mail.model.openjpa;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.Date;
import java.util.List;

import javax.mail.Flags;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Table;

import org.apache.commons.io.IOUtils;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.jpa.mail.model.JPAHeader;
import org.apache.james.mailbox.jpa.mail.model.JPAMailbox;
import org.apache.james.mailbox.store.mail.model.Message;
import org.apache.james.mailbox.store.mail.model.PropertyBuilder;
import org.apache.james.mailbox.store.streaming.LazySkippingInputStream;
import org.apache.openjpa.persistence.Persistent;

/**
 * JPA implementation of {@link AbstractJPAMessage} which use openjpas {@link Persistent} type to
 * be able to stream the message content without loading it into the memory at all. 
 * 
 * This is not supported for all DB's yet. See {@link http://openjpa.apache.org/builds/latest/docs/manual/ref_guide_mapping_jpa.html}
 * 
 * If your DB is not supported by this, use {@link JPAMessage} 
 *
 * TODO: Fix me!
 */
@Entity(name="Message")
@Table(name="JAMES_MAIL")
public class JPAStreamingMessage extends AbstractJPAMessage {

	@Persistent(optional=false, fetch=FetchType.LAZY)
	@Column(name = "MAIL_BYTES", length=1048576000, nullable = false)
	private InputStream content;
	 
    @Deprecated
    public JPAStreamingMessage() {}

    public JPAStreamingMessage(JPAMailbox mailbox, long uid, Date internalDate, int size, Flags flags, 
            InputStream content, int bodyStartOctet, final List<JPAHeader> headers, final PropertyBuilder propertyBuilder) throws MailboxException {
        super(mailbox, uid, internalDate, flags, size ,bodyStartOctet,headers,propertyBuilder);
        this.content = content;
    }

    /**
     * Create a copy of the given message
     * 
     * @param message
     * @throws IOException 
     */
    public JPAStreamingMessage(JPAMailbox mailbox, long uid,Message<?> message) throws MailboxException {
        super(mailbox, uid, message);
        try {
            this.content = new ByteArrayInputStream(IOUtils.toByteArray(message.getFullContent()));
        } catch (IOException e) {
            throw new MailboxException("Unable to parse message",e);
        }
    }

    public InputStream getFullContent() throws IOException {
        return content;
    }
    
    public InputStream getBodyContent() throws IOException {
        return new LazySkippingInputStream(content, getBodyStartOctet());
    }

}
