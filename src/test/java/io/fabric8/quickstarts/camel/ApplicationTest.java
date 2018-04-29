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

import org.apache.camel.CamelContext;
import org.apache.camel.builder.NotifyBuilder;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ApplicationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void firstBookTest() {
        HttpEntity<Book> entity = new HttpEntity<Book>(new Book(1, "1234567890", "Test book insert"));
        ResponseEntity<Book> bookResponse = restTemplate.exchange("/camel-rest-sql/books",
            HttpMethod.POST, entity, Book.class);
        assertThat(bookResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Book book = bookResponse.getBody();
        assertThat(book.getId()).isEqualTo(1);
        assertThat(book.getCode()).isEqualTo("1234567890");
        assertThat(book.getDescription()).isEqualTo("Test book insert");
    }

    @Test
    public void secondBooksTests() {
        ResponseEntity<Book> bookResponse = restTemplate.getForEntity("/camel-rest-sql/books/1", Book.class);
        assertThat(bookResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Book book = bookResponse.getBody();
        assertThat(book.getId()).isEqualTo(1);
        assertThat(book.getCode()).isEqualTo("1234567890");
        assertThat(book.getDescription()).isEqualTo("Test book insert");

        ResponseEntity<List<Book>> booksResponse = restTemplate.exchange("/camel-rest-sql/books",
            HttpMethod.GET, null, new ParameterizedTypeReference<List<Book>>(){});
        assertThat(booksResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Book> books = booksResponse.getBody();
        assertThat(books).hasSize(1);
        assertThat(books.get(0).getId()).isEqualTo(1);
        assertThat(books.get(0).getCode()).isEqualTo("1234567890");
        assertThat(books.get(0).getDescription()).isEqualTo("Test book insert");
    }

    @Test
    public void thirdBookTest() {
        HttpEntity<Book> entity = new HttpEntity<Book>(new Book(1, "1234567890", "Test book updated"));
        ResponseEntity<Book> bookResponse = restTemplate.exchange("/camel-rest-sql/books/1",
            HttpMethod.PUT, entity, Book.class);
        assertThat(bookResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Book book = bookResponse.getBody();
        assertThat(book.getId()).isEqualTo(1);
        assertThat(book.getCode()).isEqualTo("1234567890");
        assertThat(book.getDescription()).isEqualTo("Test book updated");
    }
}