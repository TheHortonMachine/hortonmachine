package eu.hydrologis.edc.annotatedclasses;

import static eu.hydrologis.edc.utils.Constants.EDC_SCHEMA;
import static eu.hydrologis.edc.utils.Constants.ID;
import static eu.hydrologis.edc.utils.Constants.NAME;
import static eu.hydrologis.edc.utils.Constants.STATUS;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * The status Table.
 * 
 * <p>
 * The table of the current state of a particular point. This might for example
 * be <i>active</i> or <i>inactive</i>.
 * </p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 */
@Entity
@Table(name = STATUS, schema = EDC_SCHEMA)
public class StatusTable {
    /**
     * The unique id of the status.
     */
    @Id
    @Column(name = ID, nullable = false)
    private Long id;

    /**
     * The name of the status.
     */
    @Column(name = NAME, nullable = false)
    private String name;

    /*
     * =================================
     */

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

}
