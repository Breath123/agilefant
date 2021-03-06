package fi.hut.soberit.agilefant.readonly;

import java.io.IOException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.filter.GenericFilterBean;

import org.hibernate.SessionFactory;
import org.hibernate.Session;

import fi.hut.soberit.agilefant.db.hibernate.IterationDAOHibernate;
import fi.hut.soberit.agilefant.db.hibernate.UserDAOHibernate;


public class ReadonlyFilter extends GenericFilterBean {
    
    @Transactional(readOnly = true)
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        
        // Our passed-in requests and responses are HttpServlet requests and responses.
        HttpServletResponse resp = (HttpServletResponse) response;
        HttpServletRequest reqt = (HttpServletRequest) request;
        
        // Create a Data Access Object instance and open a Hibernate session.
        IterationDAOHibernate iterationDao = new IterationDAOHibernate();
        UserDAOHibernate userDao = new UserDAOHibernate();
        
        SessionFactory sessionFactory;
        try {
            sessionFactory = (SessionFactory) new InitialContext().lookup("hibernateSessionFactory");
            iterationDao.setSessionFactory(sessionFactory);
            userDao.setSessionFactory(sessionFactory);
        } catch (NamingException e) {
            e.printStackTrace();
            return;
        }
        Session session = sessionFactory.openSession();
        
        // Fetch url token from request.
        String requestUrl = reqt.getRequestURL().toString();
        String token = getTokenFromUrl(requestUrl);
        session.disconnect();
        session.close();
        
        if (iterationDao.isValidReadonlyToken(token)) {
            resp.sendRedirect("/agilefant/ROIteration.action?readonlyToken=" + token);
            
        } else if (requestUrl.contains("ROIteration") && !requestUrl.endsWith("ROIteration.action")) {
            
            // I don't think this is needed but I am leaving it here for now, 
            // in case removing it does break something. - Dustin, Mar-21-2012
            //User user = userDao.getByLoginName("readonly");
            //SecurityUtil.setLoggedUser(user);
            
            try{
                chain.doFilter(request, response);
            } catch(NullPointerException npe){
                //user tried to go back in the browser after unsharing
                resp.sendRedirect("/agilefant/login.jsp");
            }
        } else {
            // Token is not valid, so redirect to login page.
            resp.sendRedirect("/agilefant/login.jsp");
        }
    }
    
    private String getTokenFromUrl(String url) {
        if(url != null) {
            int tokenStart = url.indexOf("token/");
            String token = url.substring(tokenStart + "token/".length());
            return token;
        }
        else 
            return "";
    }

}
