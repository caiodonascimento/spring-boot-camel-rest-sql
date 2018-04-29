/*
 * Copyright 2005-2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.quickstarts.camel;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.servlet.CamelHttpTransportServlet;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.stereotype.Component;

@SpringBootApplication
@ImportResource({"classpath:spring/camel-context.xml"})
public class Application extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    ServletRegistrationBean servletRegistrationBean() {
        ServletRegistrationBean servlet = new ServletRegistrationBean(
            new CamelHttpTransportServlet(), "/camel-rest-sql/*");
        servlet.setName("CamelServlet");
        return servlet;
    }

    @Component
    class RestApi extends RouteBuilder {

        @Override
        public void configure() {
            restConfiguration()
                .contextPath("/camel-rest-sql").apiContextPath("/api-doc")
                    .apiProperty("api.title", "Camel REST API")
                    .apiProperty("api.version", "1.0")
                    .apiProperty("cors", "true")
                    .apiContextRouteId("doc-api")
                .component("servlet")
                .bindingMode(RestBindingMode.json);

            rest("/books").description("Books REST service")
                .get().description("List all of registered books")
                    .produces("application/json")
                    .route().routeId("all-books-api")
                    .to("direct:get-books")
                    .endRest()
                .get("/{id}").description("Details of a book by id")
                    .produces("application/json")
                    .route().routeId("one-book-api")
                    .to("direct:get-book")
                    .endRest()
                .post().description("Insert the data of a book")
                    .type(Book.class).consumes("application/json").produces("application/json")
                    .route().routeId("save-book-api")
                    .to("direct:post-book")
                    .endRest()
                .put("/{id}").description("Update the data of a book by id")
                    .type(Book.class).consumes("application/json").produces("application/json")
                    .route().routeId("update-book-api")
                    .to("direct:put-book")
                    .endRest();
            
            from("direct:get-books")
                .to("sql:select * from books?" +
                    "dataSource=dataSource&outputClass=io.fabric8.quickstarts.camel.Book");
            
            from("direct:get-book")
                .log("Get book with ID ${header.id}")
                .choice().when(simple("${header.id} regex '^\\d+$'"))
                    .to("sql:select * from books where id = :#${header.id}?" +
                        "dataSource=dataSource&outputClass=io.fabric8.quickstarts.camel.Book")
                    .choice().when(simple("${body.isEmpty()}"))
                        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404))
                        .setHeader(Exchange.HTTP_RESPONSE_TEXT, constant("Book not registered."))
                    .otherwise()
                        .setBody(simple("${body.get(0)}"))
                    .endChoice()
                .otherwise()
                    .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
                    .setHeader(Exchange.HTTP_RESPONSE_TEXT, constant("ID of the book is necessary, it's need be numeric."))
                .endChoice();

            from("direct:post-book")
                .choice().when(simple("${body} == null"))
                    .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
                    .setHeader(Exchange.HTTP_RESPONSE_TEXT, constant("Request format is incorrect, ask backend administrator to give book data format."))        
                .otherwise()
                    .to("sql:insert into books (id, code, description) values " +
                        "(:#${body.id} , :#${body.code}, :#${body.description})?" +
                        "dataSource=dataSource")
                    .log("Inserted new book ${body.toString()}")
                .endChoice();
            
            from("direct:put-book")
                .log("Will update the book with ID ${header.id}")
                .choice().when(simple("${header.id} regex '^\\d+$'"))
                    .choice().when(simple("${body} != null"))
                        .log("${body.toString()}")
                        .to("sql:update books set code = :#${body.code}, description = :#${body.description} where id = :#${header.id}?" +
                            "dataSource=dataSource")
                    .otherwise()
                        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
                        .setHeader(Exchange.HTTP_RESPONSE_TEXT, constant("Request format is incorrect, ask backend administrator to give book data format."))        
                    .endChoice()
                .otherwise()
                    .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
                    .setHeader(Exchange.HTTP_RESPONSE_TEXT, constant("ID of the book is necessary, it's need be numeric."))
                .endChoice();
        }
    }
}