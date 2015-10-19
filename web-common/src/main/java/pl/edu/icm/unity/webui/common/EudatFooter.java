package pl.edu.icm.unity.webui.common;

import com.vaadin.server.ExternalResource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.VerticalLayout;

/**
 *
 * @author wilelb
 */
public class EudatFooter extends VerticalLayout {
    
    public EudatFooter() {
        HorizontalLayout footerLeft = new HorizontalLayout();
        footerLeft.setId("footer-left");
        footerLeft.addComponent(new Label("<img src=\"./VAADIN/themes/common/img/european-commission.jpg\">", ContentMode.HTML));
        footerLeft.addComponent(new Label("EUDAT receives funding from the European Union’s Horizon 2020 research and<br />innovation programme under grant agreement No. 654065. Legal Notice", ContentMode.HTML));

        HorizontalLayout footerRight = new HorizontalLayout();
        footerLeft.setId("footer-right");
        footerRight.addComponent(new Link("Terms of Use", new ExternalResource("http://b2access.eudat.eu/terms-of-use.html")));
        footerRight.addComponent(new Link("Data Privacy Statement", new ExternalResource("http://b2access.eudat.eu/data-privacy-statement.html")));
        footerRight.addComponent(new Link("About EUDAT", new ExternalResource("https://eudat.eu/what-eudat")));

        HorizontalLayout footer = new HorizontalLayout();
        footer.setWidth(100, Unit.PERCENTAGE);

        footer.addComponent(footerLeft);
        footer.setComponentAlignment(footerLeft, Alignment.TOP_LEFT);
        footer.addComponent(footerRight);
        footer.setComponentAlignment(footerRight, Alignment.TOP_RIGHT);

        HorizontalLayout footerUnity = new HorizontalLayout();
        footerUnity.setWidth(100, Unit.PERCENTAGE);
        Link unity = new Link("Powered by Unity-IDM", new ExternalResource("http://unity-idm.eu/"));
        footerUnity.addComponent(unity);
        footerUnity.setComponentAlignment(unity, Alignment.MIDDLE_CENTER);

        //VerticalLayout footerRows = new VerticalLayout();
        setId("footer");
        setHeightUndefined();
        setWidth(100, Unit.PERCENTAGE);
        addComponent(footer);
        addComponent(footerUnity);
    }
}
