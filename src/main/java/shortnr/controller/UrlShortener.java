package shortnr.controller;

import com.google.common.hash.Hashing;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by john on 3/27/15.
 */
@RestController
public class UrlShortener {

    @Value("${shortnr.hours-until-expiration}")
    Integer hoursUntilExpiration;

    @Autowired
    private StringRedisTemplate redis;

    @RequestMapping(value="/{id}", method = RequestMethod.GET)
    public void redirect(@PathVariable String id, HttpServletResponse response) throws Exception {
        final String url = redis.opsForValue().get(id);

        if (url != null) {
            response.sendRedirect(url);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<String> save(@RequestBody Map<String,String> json, HttpServletRequest request) {
        final String url = json.get("url");
        final UrlValidator urlValidator = new UrlValidator(new String[]{"http", "https"});

        if (urlValidator.isValid(url)) {
            final String id = Hashing.murmur3_32().hashString(url, StandardCharsets.UTF_8).toString();
            redis.opsForValue().set(id, url);
            redis.expire(id, hoursUntilExpiration, TimeUnit.HOURS);
            return new ResponseEntity<>(request.getRequestURL().toString().replace(request.getRequestURI().toString(), request.getContextPath() + "/") + id, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}
