-- Table for managing budgets linked to customers
CREATE TABLE IF NOT EXISTS `customer_budget` (
  `budget_id` int unsigned NOT NULL AUTO_INCREMENT,
  `customer_id` int unsigned NOT NULL,
  `label` varchar(255) NOT NULL,
  `amount` decimal(15,2) NOT NULL,
  `transaction_date` date NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `user_id` int DEFAULT NULL,
  PRIMARY KEY (`budget_id`),
  KEY `fk_budget_customer` (`customer_id`),
  KEY `fk_budget_user` (`user_id`),
  CONSTRAINT `fk_budget_customer` FOREIGN KEY (`customer_id`) REFERENCES `customer` (`customer_id`),
  CONSTRAINT `fk_budget_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Link table for ticket's expenses 
CREATE TABLE IF NOT EXISTS `expense` (
  `expense_id` int unsigned NOT NULL AUTO_INCREMENT,
  `amount` decimal(15,2) NOT NULL,
  `expense_date` date NOT NULL,
  PRIMARY KEY (`ticket_expense_id`),
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- taux alerte in percentage
CREATE TABLE IF NOT EXISTS `alerte_rate` (
  `alerte_rate_id` int unsigned NOT NULL AUTO_INCREMENT,
  `percentage` decimal(15,2) NOT NULL,
  `alerte_rate_date` datetime DEFAULT CURRENT_TIMESTAMP ,
  PRIMARY KEY (`alerte_rate_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- alter
ALTER TABLE `trigger_ticket` 
ADD COLUMN `expense_id` INT UNSIGNED DEFAULT NULL,
ADD CONSTRAINT `fk_ticket_expense` FOREIGN KEY (`expense_id`) REFERENCES `expense` (`expense_id`);

ALTER TABLE `trigger_lead` 
ADD COLUMN `expense_id` INT UNSIGNED DEFAULT NULL,
ADD CONSTRAINT `fk_lead_expense` FOREIGN KEY (`expense_id`) REFERENCES `expense` (`expense_id`);

INSERT INTO `alerte_rate` (percentage) VALUES (50);

INSERT INTO `expense` (`amount`, `expense_date`) VALUES
(3000000.50, '2025-08-01'),
(600000.75, '2025-08-05'),
(5500000.50, '2025-09-01'),
(2500000.75, '2025-09-05'),
(100000.00, '2025-09-10'),
(1000000.50, '2025-10-01'),
(2000000.75, '2025-10-05'),
(1500000.00, '2025-10-10'),
(3000000.25, '2025-11-01'),
(25000.50, '2025-11-15'),
(700000.00, '2025-12-01'),
(350000.75, '2025-12-10');


INSERT INTO `customer_budget` (`customer_id`, `label`, `amount`, `transaction_date`) VALUES
(1, 'Initial Budget', 1000000.00, '2025-10-01'),
(1, 'Additional Budget', 500000.00, '2025-10-15'),
(1, 'Additional Budget ii', 700000.00, '2025-11-15'),
(2, 'Project Budget', 2000000.00, '2025-10-05'),
(1, 'Q4 Budget', 150000.00, '2025-11-01'),
(2, 'Marketing Budget', 100000.00, '2025-11-10'),
(2, 'Marketing Budget ii', 450000.00, '2025-12-10'),
(1, 'Year-End Budget', 300000.00, '2025-12-01'),
(2, 'Final Budget', 200000.00, '2025-12-15');