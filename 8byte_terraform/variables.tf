variable "region" {
    description = "aws region"
    type = string
  
}
variable "vpc_cidr_block" {
    type=string

}
variable "pub_sub_cidr" {
    type = list(string)
}
variable "pri_sub_cidr" {
    type = list(string)
}
variable "availability_zones" {
    type=list(string)
}
variable "db_password" {
  type        = string
  sensitive   = true
}
