package pl.edu.icm.unity.webui.common;

import com.vaadin.server.ExternalResource;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Link;
import com.vaadin.ui.VerticalLayout;

/**
 *
 * @author wilelb
 */
public class EudatHeader extends VerticalLayout {
 
    public EudatHeader() {
        this(null);
    }
    
    public EudatHeader(TopHeaderLight subHeader) {
        
        HorizontalLayout bar = new HorizontalLayout();
        bar.setId("eudat");
        bar.setWidth(100, Unit.PERCENTAGE);
        Link l = new Link("Go to EUDAT website", new ExternalResource("http://www.eudat.eu"));
        l.setStyleName("eudat-link");
        bar.addComponent(l);
        
        addComponent(bar);
        if(subHeader != null) {
            subHeader.setId("header");
            addComponent(subHeader);
        }
    }
}
